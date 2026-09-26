package com.example.data.api

import android.net.Uri
import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CommunityApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    var currentAccessToken: String? = null
    var currentUserId: String? = null

    val isLoggedIn: Boolean
        get() = !currentAccessToken.isNullOrBlank() && !currentUserId.isNullOrBlank()

    /** Restores a previously persisted session; returns true when a session was found. */
    fun restoreSession(): Boolean {
        val saved = SessionStore.load() ?: return false
        currentAccessToken = saved.first
        currentUserId = saved.second
        return true
    }

    fun clearSession() {
        currentAccessToken = null
        currentUserId = null
        SessionStore.clear()
    }

    private fun newRequestBuilder(url: String): Request.Builder {
        val token = currentAccessToken ?: SupabaseConfig.ANON_KEY
        return Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
    }

    // ==================== AUTH & PROFILE ====================

    /** 账号标识类型：手机号或邮箱 */
    // ============ AUTH：手机号验证码（含注册）/ 密码，走 phone-auth Edge Function ============

    data class LoginResult(val profile: Profile, val isNewUser: Boolean)

    /** 口令哈希：sha256("wudian_pwd:" + 密码)，与 Web 端保持一致 */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("wudian_pwd:$password".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun normalizePhone(phone: String): String {
        val trimmed = phone.trim()
        if (trimmed.startsWith("+")) return trimmed
        return "+86${trimmed.filter { it.isDigit() }}"
    }

    private suspend fun callPhoneAuth(body: JSONObject): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = newRequestBuilder("${SupabaseConfig.FUNCTIONS_URL}/phone-auth")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            val message = json.optString("error", json.optString("msg", ""))
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(message.ifBlank { "请求失败 HTTP ${response.code}" })
                )
            }
            // 该 Edge Function 也会用 200 + success:false 返回业务错误
            if (json.optBoolean("success", true) == false || message.isNotBlank()) {
                return@withContext Result.failure(Exception(message.ifBlank { "操作失败，请稍后重试" }))
            }
            Result.success(json)
        } catch (e: Exception) {
            Log.e("ApiClient", "phone-auth error", e)
            Result.failure(e)
        }
    }

    /** 发送短信验证码（登录与注册共用）。 */
    suspend fun sendPhoneOtp(phone: String): Result<Unit> {
        val body = JSONObject().apply {
            put("action", "send_otp")
            put("phone", normalizePhone(phone))
        }
        return callPhoneAuth(body).map { Unit }
    }

    /** 校验短信验证码；手机号首次登录时后端会自动完成注册（isNewUser = true）。 */
    suspend fun verifyPhoneOtp(phone: String, code: String): Result<LoginResult> {
        val normalized = normalizePhone(phone)
        val body = JSONObject().apply {
            put("action", "verify_otp")
            put("phone", normalized)
            put("code", code.trim())
        }
        return callPhoneAuth(body).mapCatching { json ->
            val session = json.optJSONObject("session")
                ?: throw Exception("验证码校验失败，请重新获取")
            LoginResult(establishSession(session, normalized).getOrThrow(), json.optBoolean("isNewUser", false))
        }
    }

    /** 手机号 + 密码登录（密码需已在 Web 端设置过）。 */
    suspend fun passwordLogin(phone: String, password: String): Result<LoginResult> {
        val normalized = normalizePhone(phone)
        val body = JSONObject().apply {
            put("action", "password_login")
            put("phone", normalized)
            put("password_hash", hashPassword(password))
        }
        return callPhoneAuth(body).mapCatching { json ->
            val session = json.optJSONObject("session")
                ?: throw Exception("手机号或密码错误")
            LoginResult(establishSession(session, normalized).getOrThrow(), false)
        }
    }

    /** 用 phone-auth 返回的 session 建立本地会话并拉取用户资料。 */
    private suspend fun establishSession(session: JSONObject, phone: String? = null): Result<Profile> {
        val token = session.optString("access_token")
        val uid = session.optJSONObject("user")?.optString("id").orEmpty()
        if (token.isBlank() || uid.isBlank()) return Result.failure(Exception("登录会话无效，请重试"))
        currentAccessToken = token
        currentUserId = uid
        SessionStore.save(token, uid)
        // 新注册用户可能还没有 profiles 行，尽力补建一条
        val profile = getProfile(uid).getOrNull() ?: ensureProfile(uid, phone)
        if (profile == null) {
            clearSession()
            return Result.failure(Exception("无法读取当前账号资料，请稍后重试"))
        }
        return Result.success(profile)
    }


    /** 当 profiles 表缺少当前用户记录时，尝试补建。 */
    private suspend fun ensureProfile(userId: String, phone: String?): Profile? {
        return try {
            val body = JSONObject().apply {
                put("id", userId)
                if (!phone.isNullOrBlank()) put("phone", phone)
                put("nickname", phone?.takeLast(4)?.let { "雾点用户_$it" } ?: "雾点用户")
            }
            val request = newRequestBuilder("${SupabaseConfig.REST_URL}/profiles")
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) return null
            val array = JSONArray(text)
            if (array.length() > 0) parseProfile(array.getJSONObject(0)) else null
        } catch (e: Exception) {
            Log.e("ApiClient", "ensureProfile error", e)
            null
        }
    }


    fun logOut() {
        clearSession()
    }

    suspend fun getProfile(userId: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            // First try `profiles` table
            val url = "${SupabaseConfig.REST_URL}/profiles?id=eq.$userId&select=id,nickname,avatar_url,bio,phone,role,risk_status,risk_reason,is_official,created_at"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext Result.success(parseProfile(obj))
                }
            }

            // Only read public fields here; a profile must come from the private profiles table
            // so the cloud drive and role UI cannot silently use an anonymous placeholder.
            Result.failure(Exception("无法读取当前账号资料，请检查登录状态"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, nickname: String?, bio: String?, avatarUrl: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/profiles?id=eq.$userId"
            val body = JSONObject().apply {
                nickname?.let { put("nickname", it) }
                bio?.let { put("bio", it) }
                avatarUrl?.let { put("avatar_url", it) }
            }
            val request = newRequestBuilder(url)
                .addHeader("Prefer", "return=minimal")
                .patch(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== POSTS ====================

    /** 帖子查询字段（含挂载轻应用、引用帖、挂载云盘文件），与 Web 端保持一致 */
    private val postSelectFields =
        "id,user_id,group_id,title,content,ai_summary,ai_tags,ai_score,recommend_status,attachments," +
            "miniapp_id,mini_app:mini_apps!posts_miniapp_id_fkey(id,name,description,developer,icon_url,source_type,entry_url,live_version,status)," +
            "mounted_files:post_server_files(file:server_files(id,name,size_bytes))," +
            "like_count,comment_count,favorite_count,view_count,status,short_code,referenced_post_id,created_at," +
            "author:public_profiles!posts_user_id_fkey(id,nickname,avatar_url,bio)," +
            "group:groups(id,name,avatar_url)"

    suspend fun getPosts(page: Int = 0, limit: Int = 20, tagFilter: String? = null): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val offset = page * limit
            val selectFields = postSelectFields
            var url = "${SupabaseConfig.REST_URL}/posts?status=eq.published&select=$selectFields&order=created_at.desc&limit=$limit&offset=$offset"
            if (!tagFilter.isNullOrBlank() && tagFilter != "全部") {
                url += "&ai_tags=cs.{${Uri.encode(tagFilter)}}"
            }
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val posts = mutableListOf<Post>()
                for (i in 0 until array.length()) {
                    posts.add(parsePost(array.getJSONObject(i)))
                }
                Result.success(posts)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "getPosts error", e)
            Result.failure(e)
        }
    }

    suspend fun getPostDetail(postId: String): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/posts?id=eq.$postId&select=$postSelectFields"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val post = parsePost(array.getJSONObject(0))
                    // 引用帖数据来自 post_reference_view 视图
                    val referencedId = post.referenced_post_id
                    if (!referencedId.isNullOrBlank()) {
                        Result.success(post.copy(referenced_post = fetchReferencedPost(postId)))
                    } else {
                        Result.success(post)
                    }
                } else {
                    Result.failure(Exception("帖子不存在或已删除"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 取被引用帖子的摘要信息（视图 post_reference_view） */
    private fun fetchReferencedPost(postId: String): ReferencedPost? {
        return try {
            val url = "${SupabaseConfig.REST_URL}/post_reference_view?post_id=eq.$postId&select=ref_id,ref_title,ref_status,ref_summary,ref_author_id,ref_author_nickname,ref_author_avatar&limit=1"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            if (!response.isSuccessful) return null
            val array = JSONArray(body)
            if (array.length() == 0) null else parseReferencedPost(array.getJSONObject(0))
        } catch (e: Exception) {
            Log.e("ApiClient", "fetchReferencedPost error", e)
            null
        }
    }

    suspend fun createPost(userId: String, title: String, content: String, tags: List<String>, attachmentUrl: String?): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/posts"
            val body = JSONObject().apply {
                put("user_id", userId)
                put("title", title)
                put("content", content)
                put("status", "published")
                if (tags.isNotEmpty()) {
                    put("ai_tags", JSONArray(tags))
                }
                if (!attachmentUrl.isNullOrBlank()) {
                    val attachArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("url", attachmentUrl)
                        })
                    }
                    put("attachments", attachArray)
                }
            }
            val request = newRequestBuilder(url)
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(respStr)
                if (array.length() > 0) {
                    Result.success(parsePost(array.getJSONObject(0)))
                } else {
                    Result.success(Post(title = title, content = content, user_id = userId))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: $respStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 积分红包（points-proxy） ====================

    private suspend fun callPointsProxy(body: JSONObject): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = newRequestBuilder("${SupabaseConfig.FUNCTIONS_URL}/points-proxy")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            val message = json.optString("error", json.optString("msg", ""))
            if (!response.isSuccessful || message.isNotBlank()) {
                return@withContext Result.failure(Exception(message.ifBlank { "请求失败 HTTP ${response.code}" }))
            }
            Result.success(json)
        } catch (e: Exception) {
            Log.e("ApiClient", "points-proxy error", e)
            Result.failure(e)
        }
    }

    /** 查询帖子/公告挂载的红包，没有则返回 null */
    suspend fun getRedPacket(postId: String? = null, announcementId: String? = null): Result<RedPacket?> {
        val body = JSONObject().apply {
            put("action", "redpacket_get")
            if (!postId.isNullOrBlank()) put("post_id", postId)
            if (!announcementId.isNullOrBlank()) put("announcement_id", announcementId)
        }
        return callPointsProxy(body).map { json ->
            val packet = json.optJSONObject("packet") ?: return@map null
            parseRedPacket(packet)
        }
    }

    /** 抢红包，返回需打开的积分接收地址 */
    suspend fun grabRedPacket(packetId: String): Result<RedPacketGrabResult> {
        val body = JSONObject().apply {
            put("action", "redpacket_grab")
            put("packet_id", packetId)
        }
        return callPointsProxy(body).map { json ->
            RedPacketGrabResult(
                claim_id = json.optString("claim_id", ""),
                amount = json.optInt("amount", 0),
                auth_url = json.optString("auth_url", "")
            )
        }
    }

    private fun parseRedPacket(obj: JSONObject): RedPacket {
        val claims = mutableListOf<RedPacketClaim>()
        obj.optJSONArray("claims")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                claims.add(
                    RedPacketClaim(
                        amount = c.optInt("amount", 0),
                        nickname = c.optString("nickname", null),
                        avatar_url = c.optString("avatar_url", null),
                        confirmed_at = c.optString("confirmed_at", null)
                    )
                )
            }
        }
        val mine = obj.optJSONObject("my_claim")?.let {
            RedPacketClaim(
                amount = it.optInt("amount", 0),
                nickname = it.optString("nickname", null),
                avatar_url = it.optString("avatar_url", null),
                confirmed_at = it.optString("confirmed_at", null)
            )
        }
        return RedPacket(
            id = obj.optString("id", ""),
            type = obj.optString("type", "lucky"),
            status = obj.optString("status", ""),
            sender_id = obj.optString("sender_id", null),
            created_at = obj.optString("created_at", null),
            expires_at = obj.optString("expires_at", null),
            total_count = obj.optInt("total_count", 0),
            unit_amount = if (obj.has("unit_amount") && !obj.isNull("unit_amount")) obj.optInt("unit_amount") else null,
            remain_count = obj.optInt("remain_count", 0),
            total_amount = obj.optInt("total_amount", 0),
            remain_amount = obj.optInt("remain_amount", 0),
            has_commented = obj.optBoolean("has_commented", false),
            claims = if (claims.isNotEmpty()) claims else null,
            my_claim = mine
        )
    }

    // ==================== TAGS ====================

    /** 从最新已发布帖子中聚合标签，供首页分类筛选使用（不再使用本地硬编码分类）。 */
    suspend fun getRecentTagOptions(limit: Int = 100): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/posts?status=eq.published&select=ai_tags&order=created_at.desc&limit=$limit"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
            val array = JSONArray(body)
            val counts = LinkedHashMap<String, Int>()
            for (i in 0 until array.length()) {
                val tags = array.getJSONObject(i).optJSONArray("ai_tags") ?: continue
                for (j in 0 until tags.length()) {
                    val tag = tags.optString(j).trim()
                    if (tag.isNotEmpty()) counts[tag] = (counts[tag] ?: 0) + 1
                }
            }
            Result.success(counts.entries.sortedByDescending { it.value }.map { it.key })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementPostViews(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/post_view_log"
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val uid = currentUserId ?: return@withContext false
            val body = JSONObject().apply {
                put("post_id", postId)
                put("user_id", uid)
                put("viewed_at", sdf.format(Date()))
            }
            val request = newRequestBuilder(url)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun togglePostLike(postId: String): Result<Pair<Boolean, Int>> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserId ?: return@withContext Result.failure(Exception("请先登录后再点赞"))
            val checkUrl = "${SupabaseConfig.REST_URL}/post_likes?post_id=eq.$postId&user_id=eq.$uid&select=post_id"
            val checkReq = newRequestBuilder(checkUrl).get().build()
            val checkResp = client.newCall(checkReq).execute()
            val checkBody = checkResp.body?.string() ?: ""

            val alreadyLiked = checkResp.isSuccessful && JSONArray(checkBody).length() > 0
            if (alreadyLiked) {
                // Delete like
                val delUrl = "${SupabaseConfig.REST_URL}/post_likes?post_id=eq.$postId&user_id=eq.$uid"
                val delReq = newRequestBuilder(delUrl).delete().build()
                client.newCall(delReq).execute()
                Result.success(Pair(false, 0))
            } else {
                // Insert like
                val insUrl = "${SupabaseConfig.REST_URL}/post_likes"
                val body = JSONObject().apply {
                    put("post_id", postId)
                    put("user_id", uid)
                }
                val insReq = newRequestBuilder(insUrl).post(body.toString().toRequestBody(jsonMediaType)).build()
                client.newCall(insReq).execute()
                Result.success(Pair(true, 1))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePostFavorite(postId: String): Result<Pair<Boolean, Int>> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserId ?: return@withContext Result.failure(Exception("请先登录后再收藏"))
            val checkUrl = "${SupabaseConfig.REST_URL}/post_favorites?post_id=eq.$postId&user_id=eq.$uid&select=post_id"
            val checkReq = newRequestBuilder(checkUrl).get().build()
            val checkResp = client.newCall(checkReq).execute()
            val checkBody = checkResp.body?.string() ?: ""

            val alreadyFav = checkResp.isSuccessful && JSONArray(checkBody).length() > 0
            if (alreadyFav) {
                // Delete favorite
                val delUrl = "${SupabaseConfig.REST_URL}/post_favorites?post_id=eq.$postId&user_id=eq.$uid"
                val delReq = newRequestBuilder(delUrl).delete().build()
                client.newCall(delReq).execute()
                Result.success(Pair(false, 0))
            } else {
                // Insert favorite
                val insUrl = "${SupabaseConfig.REST_URL}/post_favorites"
                val body = JSONObject().apply {
                    put("post_id", postId)
                    put("user_id", uid)
                }
                val insReq = newRequestBuilder(insUrl).post(body.toString().toRequestBody(jsonMediaType)).build()
                client.newCall(insReq).execute()
                Result.success(Pair(true, 1))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== COMMENTS ====================

    suspend fun getComments(postId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/comments?post_id=eq.$postId&select=id,post_id,user_id,parent_id,root_comment_id,reply_to_user_id,content,created_at,author:public_profiles!comments_user_id_fkey(id,nickname,avatar_url)&order=created_at.asc"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val comments = mutableListOf<Comment>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    comments.add(parseComment(obj))
                }
                Result.success(comments)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(postId: String, userId: String, content: String): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/comments"
            val body = JSONObject().apply {
                put("post_id", postId)
                put("user_id", userId)
                put("content", content)
            }
            val request = newRequestBuilder(url)
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(respStr)
                if (array.length() > 0) {
                    Result.success(parseComment(array.getJSONObject(0)))
                } else {
                    Result.success(Comment(post_id = postId, user_id = userId, content = content))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: $respStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== SEARCH ====================

    suspend fun searchPosts(keyword: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val kw = Uri.encode(keyword.trim())
            val selectFields = postSelectFields
            val url = "${SupabaseConfig.REST_URL}/posts?status=eq.published&or=(title.ilike.%25$kw%25,content.ilike.%25$kw%25,ai_summary.ilike.%25$kw%25)&select=$selectFields&order=like_count.desc&limit=20"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<Post>()
                for (i in 0 until array.length()) {
                    list.add(parsePost(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProfiles(keyword: String): Result<List<Profile>> = withContext(Dispatchers.IO) {
        try {
            val kw = Uri.encode(keyword.trim())
            val url = "${SupabaseConfig.REST_URL}/public_profiles?or=(nickname.ilike.%25$kw%25,bio.ilike.%25$kw%25)&select=id,nickname,avatar_url,bio,gender,interests,created_at&limit=20"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<Profile>()
                for (i in 0 until array.length()) {
                    list.add(parseProfile(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== MINI-APPS ====================

    suspend fun getMiniApps(): Result<List<MiniApp>> = withContext(Dispatchers.IO) {
        try {
            // 真实表名 mini_apps，已上架状态为 approved（与 Web 端一致）
            val url = "${SupabaseConfig.REST_URL}/mini_apps?status=eq.approved&select=id,name,description,developer,icon_url,source_type,entry_url,live_version,status,updated_at&order=updated_at.desc&limit=50"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<MiniApp>()
                for (i in 0 until array.length()) {
                    list.add(parseMiniApp(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 单个小程序（用于取运行地址） */
    suspend fun getMiniApp(miniAppId: String): Result<MiniApp> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/mini_apps?id=eq.${Uri.encode(miniAppId)}&select=id,name,description,developer,icon_url,source_type,entry_url,storage_path,live_version,status,updated_at&limit=1"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
            val array = JSONArray(body)
            if (array.length() == 0) Result.failure(Exception("该小程序不存在或未上架"))
            else Result.success(parseMiniApp(array.getJSONObject(0)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== CLOUD DRIVE ====================

    suspend fun getUserServer(userId: String): Result<UserServer> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/user_servers?owner_id=eq.${Uri.encode(userId)}&select=id,name,port,quota_bytes,used_bytes,status,created_at&limit=1"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("云盘查询失败 HTTP ${response.code}: $body"))
            val array = JSONArray(body)
            if (array.length() > 0) Result.success(parseUserServer(array.getJSONObject(0)))
            else Result.failure(Exception("当前账号尚无云盘实例，请先在网页端开通"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServerFolders(serverId: String): Result<List<ServerFolder>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/server_folders?server_id=eq.${Uri.encode(serverId)}&select=id,server_id,name,created_at&order=name.asc"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<ServerFolder>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(ServerFolder(
                        id = obj.optString("id", ""),
                        server_id = obj.optString("server_id", serverId),
                        owner_id = obj.optString("owner_id", null),
                        name = obj.optString("name", "未命名文件夹"),
                        created_at = obj.optString("created_at")
                    ))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServerFiles(serverId: String, folderId: String? = null): Result<List<ServerFile>> = withContext(Dispatchers.IO) {
        try {
            val filter = if (folderId != null) "folder_id=eq.${Uri.encode(folderId)}" else "folder_id=is.null"
            val url = "${SupabaseConfig.REST_URL}/server_files?server_id=eq.${Uri.encode(serverId)}&$filter&select=id,server_id,folder_id,name,size_bytes,storage_path,mime_type,download_count,created_at&order=created_at.desc"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<ServerFile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseServerFile(obj))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createShareLink(fileId: String, password: String?, maxDownloads: Int = 10, expiresIn: String = "1d"): Result<ShareResult> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.FUNCTIONS_URL}/server-file"
            val body = JSONObject().apply {
                put("action", "create_share")
                put("fileId", fileId)
                if (!password.isNullOrBlank()) put("password", password)
                put("maxDownloads", maxDownloads)
                put("expiresIn", expiresIn)
            }
            val request = newRequestBuilder(url)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(respStr)
                val code = json.optString("code", "")
                if (code.isBlank() || json.optBoolean("success", true) == false || json.has("error")) {
                    return@withContext Result.failure(Exception(json.optString("error", "服务端未返回有效提取码")))
                }
                val shareUrl = json.optString("shareUrl").ifBlank { "${SupabaseConfig.WEB_BASE_URL}/share/$code" }
                val expiresAt = json.optString("expiresAt", "")
                Result.success(ShareResult(code = code, shareUrl = shareUrl, expiresAt = expiresAt))
            } else {
                Result.failure(Exception("创建分享失败 HTTP ${response.code}: $respStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShareInfo(code: String): Result<ShareInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.FUNCTIONS_URL}/server-file"
            val body = JSONObject().apply {
                put("action", "share_info")
                put("code", code)
            }
            val request = newRequestBuilder(url)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(respStr)
                if (json.has("error") || json.optBoolean("success", true) == false) {
                    return@withContext Result.failure(Exception(json.optString("error", "分享已失效或提取码无效")))
                }
                Result.success(ShareInfo(
                    fileName = json.optString("name", json.optString("fileName", "共享文件_$code")),
                    fileSizeBytes = json.optLong("size_bytes", json.optLong("fileSizeBytes", 0L)),
                    needPassword = json.optBoolean("needPassword", json.optBoolean("hasPassword", false)),
                    isExpired = json.optBoolean("isExpired", false),
                    remainingDownloads = json.optInt("remainingDownloads", -1)
                ))
            } else {
                Result.failure(Exception("获取分享信息失败 HTTP ${response.code}: $respStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadShare(code: String, password: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.FUNCTIONS_URL}/server-file"
            val body = JSONObject().apply {
                put("action", "share_download")
                put("code", code)
                if (!password.isNullOrBlank()) put("password", password)
            }
            val request = newRequestBuilder(url)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(respStr)
                if (json.has("error") || json.optBoolean("success", true) == false) {
                    return@withContext Result.failure(Exception(json.optString("error", "提取失败")))
                }
                val downloadUrl = json.optString("downloadUrl", json.optString("url", ""))
                if (downloadUrl.isNotBlank()) {
                    Result.success(downloadUrl)
                } else {
                    Result.failure(Exception("未能解析下载直链"))
                }
            } else {
                Result.failure(Exception("下载提取失败 HTTP ${response.code}: $respStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== GROUPS & CHAT ====================

    suspend fun getGroups(): Result<List<Group>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/groups?select=id,name,description,avatar_url,owner_id,announcement,created_at,owner:public_profiles!groups_owner_id_fkey(id,nickname,avatar_url),member_count:group_members(count)&order=created_at.desc"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<Group>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseGroup(obj))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGroupMessages(groupId: String): Result<List<GroupMessage>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/group_messages?group_id=eq.$groupId&select=id,group_id,user_id,content,created_at,sender:public_profiles!group_messages_user_id_fkey(id,nickname,avatar_url)&order=created_at.asc&limit=50"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<GroupMessage>()
                for (i in 0 until array.length()) {
                    list.add(parseGroupMessage(array.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendGroupMessage(groupId: String, userId: String, content: String): Result<GroupMessage> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/group_messages"
            val body = JSONObject().apply {
                put("group_id", groupId)
                put("user_id", userId)
                put("content", content)
            }
            val request = newRequestBuilder(url)
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(respStr)
                if (array.length() > 0) {
                    Result.success(parseGroupMessage(array.getJSONObject(0)))
                } else {
                    Result.success(GroupMessage(group_id = groupId, user_id = userId, content = content))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: $respStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ANNOUNCEMENTS & FEEDBACK ====================

    suspend fun getAnnouncements(): Result<List<Announcement>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/announcements?select=id,title,content,images,video_url,linked_post_id,linked_miniapp_id,created_by,created_at&order=created_at.desc&limit=10"
            val request = newRequestBuilder(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<Announcement>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseAnnouncement(obj))
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitFeedback(userId: String, type: String, title: String, content: String, contact: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/feedback"
            val formattedContent = if (title.isNotBlank()) "【$type】$title\n$content" else "【$type】$content"
            val body = JSONObject().apply {
                put("user_id", userId)
                put("content", formattedContent)
                contact?.let { put("contact", it) }
            }
            val request = newRequestBuilder(url)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ADMIN ACTIONS ====================

    suspend fun deletePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/posts?id=eq.$postId"
            val request = newRequestBuilder(url).delete().build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRiskStatus(targetUserId: String, status: String, reason: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_URL}/profiles?id=eq.$targetUserId"
            val body = JSONObject().apply {
                put("risk_status", status)
                reason?.let { put("risk_reason", it) }
            }
            val request = newRequestBuilder(url)
                .patch(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== JSON PARSERS ====================

    private fun parseProfile(obj: JSONObject): Profile {
        return Profile(
            id = obj.optString("id", ""),
            nickname = obj.optString("nickname", null),
            avatar_url = obj.optString("avatar_url", null),
            bio = obj.optString("bio", null),
            phone = obj.optString("phone", null),
            role = obj.optString("role", "user"),
            risk_status = obj.optString("risk_status", "normal"),
            risk_reason = obj.optString("risk_reason", null),
            is_official = obj.optBoolean("is_official", false),
            created_at = obj.optString("created_at", null)
        )
    }

    private fun parseAuthor(obj: JSONObject?): Author? {
        if (obj == null) return null
        return Author(
            id = obj.optString("id", ""),
            nickname = obj.optString("nickname", null),
            avatar_url = obj.optString("avatar_url", null),
            bio = obj.optString("bio", null),
            role = obj.optString("role", "user")
        )
    }

    private fun parsePost(obj: JSONObject): Post {
        val tags = mutableListOf<String>()
        val tagArray = obj.optJSONArray("ai_tags")
        if (tagArray != null) {
            for (i in 0 until tagArray.length()) {
                tags.add(tagArray.optString(i))
            }
        }
        val attachments = mutableListOf<PostAttachment>()
        val attachArray = obj.optJSONArray("attachments")
        if (attachArray != null) {
            for (i in 0 until attachArray.length()) {
                val aObj = attachArray.optJSONObject(i)
                if (aObj != null) {
                    attachments.add(PostAttachment(
                        type = aObj.optString("type", "image"),
                        url = aObj.optString("url", ""),
                        name = aObj.optString("name", null)
                    ))
                }
            }
        }
        val mountedFiles = mutableListOf<PostMountedFile>()
        obj.optJSONArray("mounted_files")?.let { arr ->
            for (i in 0 until arr.length()) {
                val fileObj = arr.optJSONObject(i)?.optJSONObject("file") ?: continue
                mountedFiles.add(
                    PostMountedFile(
                        file = MountedFileRef(
                            id = fileObj.optString("id", ""),
                            name = fileObj.optString("name", ""),
                            size_bytes = fileObj.optLong("size_bytes", 0L)
                        )
                    )
                )
            }
        }

        return Post(
            id = obj.optString("id", ""),
            user_id = obj.optString("user_id", ""),
            group_id = obj.optString("group_id", null),
            title = obj.optString("title", ""),
            content = obj.optString("content", ""),
            ai_summary = obj.optString("ai_summary", null),
            ai_tags = if (tags.isNotEmpty()) tags else null,
            ai_score = if (obj.has("ai_score") && !obj.isNull("ai_score")) obj.optInt("ai_score") else null,
            recommend_status = obj.optString("recommend_status", "normal"),
            attachments = if (attachments.isNotEmpty()) attachments else null,
            miniapp_id = obj.optString("miniapp_id", null),
            mini_app = obj.optJSONObject("mini_app")?.let { parseMiniApp(it) },
            referenced_post_id = obj.optString("referenced_post_id", null),
            mounted_files = if (mountedFiles.isNotEmpty()) mountedFiles else null,
            like_count = obj.optInt("like_count", 0),
            comment_count = obj.optInt("comment_count", 0),
            favorite_count = obj.optInt("favorite_count", 0),
            view_count = obj.optInt("view_count", 0),
            status = obj.optString("status", "published"),
            short_code = obj.optString("short_code", null),
            created_at = obj.optString("created_at", null),
            author = parseAuthor(obj.optJSONObject("author"))
        )
    }

    private fun parseReferencedPost(obj: JSONObject): ReferencedPost = ReferencedPost(
        ref_id = obj.optString("ref_id", ""),
        ref_title = obj.optString("ref_title", null),
        ref_status = obj.optString("ref_status", null),
        ref_summary = obj.optString("ref_summary", null),
        ref_author_id = obj.optString("ref_author_id", null),
        ref_author_nickname = obj.optString("ref_author_nickname", null),
        ref_author_avatar = obj.optString("ref_author_avatar", null)
    )

    private fun parseComment(obj: JSONObject): Comment {
        return Comment(
            id = obj.optString("id", ""),
            post_id = obj.optString("post_id", ""),
            user_id = obj.optString("user_id", ""),
            parent_id = obj.optString("parent_id", null),
            root_comment_id = obj.optString("root_comment_id", null),
            reply_to_user_id = obj.optString("reply_to_user_id", null),
            content = obj.optString("content", ""),
            created_at = obj.optString("created_at", null),
            author = parseAuthor(obj.optJSONObject("author"))
        )
    }

    private fun parseMiniApp(obj: JSONObject): MiniApp {
        return MiniApp(
            id = obj.optString("id", ""),
            name = obj.optString("name", ""),
            description = obj.optString("description", null),
            icon_url = obj.optString("icon_url", null),
            developer = obj.optString("developer", null),
            live_version = obj.optInt("live_version", 1),
            status = obj.optString("status", "approved"),
            source_type = obj.optString("source_type", null),
            entry_url = obj.optString("entry_url", null),
            storage_path = obj.optString("storage_path", null),
            created_at = obj.optString("created_at", null),
            updated_at = obj.optString("updated_at", null)
        )
    }

    private fun parseUserServer(obj: JSONObject): UserServer {
        return UserServer(
            id = obj.optString("id", ""),
            owner_id = obj.optString("owner_id", null),
            name = obj.optString("name", "我的云盘"),
            port = if (obj.has("port") && !obj.isNull("port")) obj.optInt("port") else null,
            quota_bytes = obj.optLong("quota_bytes", 10485760L),
            used_bytes = obj.optLong("used_bytes", 0L),
            status = obj.optString("status", "active"),
            created_at = obj.optString("created_at", null)
        )
    }

    private fun parseServerFile(obj: JSONObject): ServerFile {
        val name = obj.optString("name", "文件")
        val inferredMime = when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "json" -> "application/json"
            "txt", "md" -> "text/plain"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
        return ServerFile(
            id = obj.optString("id", ""),
            server_id = obj.optString("server_id", ""),
            owner_id = obj.optString("owner_id", null),
            folder_id = obj.optString("folder_id", null),
            name = name,
            size_bytes = obj.optLong("size_bytes", 0L),
            storage_path = obj.optString("storage_path", ""),
            mime_type = obj.optString("mime_type").takeIf { it.isNotBlank() } ?: inferredMime,
            download_count = obj.optInt("download_count", 0),
            created_at = obj.optString("created_at", null)
        )
    }

    private fun parseGroup(obj: JSONObject): Group {
        val memberCount = when (val mc = obj.opt("member_count")) {
            is JSONArray -> mc.optJSONObject(0)?.optInt("count", 1) ?: 1
            is Number -> mc.toInt()
            else -> obj.optInt("member_count", 1)
        }
        return Group(
            id = obj.optString("id", ""),
            name = obj.optString("name", ""),
            description = obj.optString("description", null),
            avatar_url = obj.optString("avatar_url", null),
            member_count = memberCount,
            owner_id = obj.optString("owner_id", null),
            announcement = obj.optString("announcement", null),
            created_at = obj.optString("created_at", null)
        )
    }

    private fun parseGroupMessage(obj: JSONObject): GroupMessage {
        return GroupMessage(
            id = obj.optString("id", ""),
            group_id = obj.optString("group_id", ""),
            user_id = obj.optString("user_id", ""),
            content = obj.optString("content", ""),
            created_at = obj.optString("created_at", null),
            sender = parseAuthor(obj.optJSONObject("sender"))
        )
    }

    private fun parseAnnouncement(obj: JSONObject): Announcement {
        val images = mutableListOf<String>()
        val imgArray = obj.optJSONArray("images")
        if (imgArray != null) {
            for (i in 0 until imgArray.length()) {
                images.add(imgArray.optString(i))
            }
        }
        return Announcement(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            content = obj.optString("content", ""),
            images = if (images.isNotEmpty()) images else null,
            video_url = obj.optString("video_url", null),
            linked_post_id = obj.optString("linked_post_id", null),
            linked_miniapp_id = obj.optString("linked_miniapp_id", null),
            created_at = obj.optString("created_at", null)
        )
    }
}

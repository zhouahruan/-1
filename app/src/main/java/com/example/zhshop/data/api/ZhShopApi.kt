package com.example.zhshop.data.api

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ZhShopApi {
    // 二、用户账户与认证模块
    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<ResponseBody>

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<ResponseBody>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): Response<ResponseBody>

    @GET("auth/me")
    suspend fun getMe(): Response<ResponseBody>

    @PUT("auth/me")
    suspend fun updateMe(@Body body: Map<String, Any>): Response<ResponseBody>

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<ResponseBody>

    @POST("auth/logout")
    suspend fun logout(): Response<ResponseBody>

    // 三、应用商店与首页核心模块
    @GET("pulse")
    suspend fun getPulse(): Response<ResponseBody>

    @GET("android/apps")
    suspend fun getApps(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("category_id") categoryId: String? = null
    ): Response<ResponseBody>

    @GET("android/apps/{id}")
    suspend fun getAppDetail(@Path("id") id: String): Response<ResponseBody>

    @GET("android/categories")
    suspend fun getCategories(): Response<ResponseBody>

    @GET("search")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ResponseBody>

    @GET("leaderboard/{type}")
    suspend fun getLeaderboard(
        @Path("type") type: String,
        @Query("limit") limit: Int = 20
    ): Response<ResponseBody>

    @GET("announcements")
    suspend fun getAnnouncements(): Response<ResponseBody>

    @POST("visitors")
    suspend fun reportVisitor(@Body body: Map<String, Any>): Response<ResponseBody>

    // 四、开发者与应用生命周期管理
    @GET("developer/apps")
    suspend fun getDeveloperApps(): Response<ResponseBody>

    @PUT("developer/apps/{id}")
    suspend fun updateDeveloperApp(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ResponseBody>

    @POST("developer/apps/{id}/unpublish")
    suspend fun unpublishApp(@Path("id") id: String): Response<ResponseBody>

    @POST("developer/apps/{id}/republish")
    suspend fun republishApp(@Path("id") id: String): Response<ResponseBody>

    @POST("developer/apps/{id}/version-updates")
    suspend fun submitVersionUpdate(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ResponseBody>

    @GET("admin/version-updates")
    suspend fun getAdminVersionUpdates(): Response<ResponseBody>

    @POST("admin/version-updates/{id}/review")
    suspend fun reviewVersionUpdate(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ResponseBody>

    @GET("rejected-apps")
    suspend fun getRejectedApps(): Response<ResponseBody>

    @GET("rejected-apps/{id}/comments")
    suspend fun getRejectedComments(@Path("id") id: String): Response<ResponseBody>

    @POST("rejected-apps/{id}/comments")
    suspend fun postRejectedComment(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("rejected-apps/{id}/appeals")
    suspend fun submitAppeal(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    // 五、社区论坛与社交系统
    @GET("forum/boards")
    suspend fun getForumBoards(): Response<ResponseBody>

    @GET("forum/posts")
    suspend fun getForumPosts(
        @Query("board_id") boardId: String? = null
    ): Response<ResponseBody>

    @POST("forum/posts")
    suspend fun createPost(@Body body: Map<String, Any>): Response<ResponseBody>

    @GET("forum/posts/{id}")
    suspend fun getPostDetail(@Path("id") id: String): Response<ResponseBody>

    @GET("forum/posts/{id}/replies")
    suspend fun getPostReplies(@Path("id") id: String): Response<ResponseBody>

    @POST("forum/posts/{id}/replies")
    suspend fun createPostReply(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("forum/checkin")
    suspend fun checkin(): Response<ResponseBody>

    @GET("forum/checkin/calendar")
    suspend fun getCheckinCalendar(): Response<ResponseBody>

    @GET("forum/points")
    suspend fun getPoints(): Response<ResponseBody>

    @GET("forum/badges")
    suspend fun getBadges(): Response<ResponseBody>

    @GET("social/users/{id}/public-profile")
    suspend fun getPublicProfile(@Path("id") id: String): Response<ResponseBody>

    @GET("social/users/{id}/followers")
    suspend fun getFollowers(@Path("id") id: String): Response<ResponseBody>

    @GET("social/users/{id}/following")
    suspend fun getFollowing(@Path("id") id: String): Response<ResponseBody>

    @POST("social/users/{id}/follow")
    suspend fun followUser(@Path("id") id: String): Response<ResponseBody>

    @DELETE("social/users/{id}/follow")
    suspend fun unfollowUser(@Path("id") id: String): Response<ResponseBody>

    @GET("social/feed")
    suspend fun getSocialFeed(): Response<ResponseBody>

    @POST("social/reviews/{id}/like")
    suspend fun likeReview(@Path("id") id: String): Response<ResponseBody>

    @DELETE("social/reviews/{id}/like")
    suspend fun unlikeReview(@Path("id") id: String): Response<ResponseBody>

    @GET("social/reviews/{id}/replies")
    suspend fun getReviewReplies(@Path("id") id: String): Response<ResponseBody>

    @POST("social/reviews/{id}/replies")
    suspend fun createReviewReply(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @GET("social/collections")
    suspend fun getCollections(): Response<ResponseBody>

    @POST("social/collections/{id}/subscribe")
    suspend fun subscribeCollection(@Path("id") id: String): Response<ResponseBody>

    @DELETE("social/collections/{id}/subscribe")
    suspend fun unsubscribeCollection(@Path("id") id: String): Response<ResponseBody>

    // 六、即时通讯与在线状态
    @POST("chat/presence")
    suspend fun reportPresence(@Body body: Map<String, String>): Response<ResponseBody>

    @GET("chat/presence")
    suspend fun getPresenceBatch(@Query("user_ids") userIds: String): Response<ResponseBody>

    @GET("chat/conversations")
    suspend fun getConversations(): Response<ResponseBody>

    @POST("chat/conversations")
    suspend fun createConversation(@Body body: Map<String, String>): Response<ResponseBody>

    @GET("chat/conversations/{id}")
    suspend fun getConversationDetail(@Path("id") id: String): Response<ResponseBody>

    @GET("chat/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("page_size") pageSize: Int = 50,
        @Query("before_id") beforeId: String? = null
    ): Response<ResponseBody>

    @POST("chat/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @GET("chat/conversations/{id}/files")
    suspend fun getChatFiles(@Path("id") id: String): Response<ResponseBody>

    // 七、系统消息与通知推送
    @GET("notifications")
    suspend fun getNotifications(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ResponseBody>

    @GET("update-notifications")
    suspend fun getUpdateNotifications(): Response<ResponseBody>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ResponseBody>

    // 八、赞助与商业化申请系统
    @POST("sponsor/apply")
    suspend fun applySponsor(@Body body: Map<String, Any>): Response<ResponseBody>

    @GET("sponsor/my-applications")
    suspend fun getMySponsorApplications(): Response<ResponseBody>

    @POST("admin/sponsor/applications/{id}/review")
    suspend fun reviewSponsorApplication(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ResponseBody>

    @POST("admin/leaderboard/annotation")
    suspend fun setLeaderboardAnnotation(@Body body: Map<String, String>): Response<ResponseBody>

    // 九、对象存储与遥测
    @POST("oss/sign")
    suspend fun getOssSign(@Body body: Map<String, String>): Response<ResponseBody>

    @POST("common-data")
    suspend fun reportTelemetry(@Body body: Map<String, Any>): Response<ResponseBody>

    // 十、客户端版本检查
    @GET("zhshop-updates/latest")
    suspend fun getClientLatestUpdate(): Response<ResponseBody>
}

interface ApkParserApi {
    @POST("apk-parser")
    suspend fun parseApk(@Body body: Map<String, String>): Response<ResponseBody>
}

interface LiuYunApi {
    @FormUrlEncoded
    @POST("api/index.php")
    suspend fun requestLiuYun(
        @Field("act") act: String,
        @FieldMap params: Map<String, String> = emptyMap()
    ): Response<ResponseBody>
}

object ApiClient {
    const val BASE_URL_GATEWAY =
        "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/"
    const val BASE_URL_APK_PARSER =
        "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/"
    const val BASE_URL_LIUYUN =
        "http://ly.jianmo.icu/"

    fun createZhShopApi(tokenManager: TokenManager): ZhShopApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val token = tokenManager.getAccessToken()
                if (!token.isNullOrBlank() && token != "default_dev_token_zhshop_2026") {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                requestBuilder.addHeader("Accept", "application/json")
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL_GATEWAY)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        return retrofit.create(ZhShopApi::class.java)
    }

    fun createApkParserApi(): ApkParserApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL_APK_PARSER)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApkParserApi::class.java)
    }

    fun createLiuYunApi(): LiuYunApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL_LIUYUN)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(LiuYunApi::class.java)
    }
}

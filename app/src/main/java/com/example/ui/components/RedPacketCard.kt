package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RedPacket

private val RedStart = Color(0xFFD93025)
private val RedEnd = Color(0xFFB82323)

/**
 * 积分红包卡片（后端 points-proxy: redpacket_get / redpacket_grab）。
 * 展示红包总额、剩余个数、领取名单，并提供抢红包入口。
 */
@Composable
fun RedPacketCard(
    packet: RedPacket,
    grabbing: Boolean,
    onGrab: (RedPacket) -> Unit,
    modifier: Modifier = Modifier,
    showCommentHint: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .testTag("red_packet_card")
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(RedStart, RedEnd)))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = "红包",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (packet.type == "lucky") "拼手气积分红包" else "积分红包",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "共 ${packet.total_amount} 积分 · ${packet.total_count} 个",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = packet.statusText,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            if (showCommentHint && !packet.has_commented && packet.isGrabbable) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "提示：评论本帖后可参与领取",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            val claims = packet.claims.orEmpty()
            if (claims.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                claims.take(3).forEach { claim ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        UserAvatar(
                            avatarUrl = claim.avatar_url,
                            name = claim.nickname ?: "雾友",
                            size = 20.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = claim.nickname ?: "雾友",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "+${claim.amount} 积分",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (claims.size > 3) {
                    Text(
                        text = "等 ${claims.size} 人已领取",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onGrab(packet) },
                enabled = packet.isGrabbable && !grabbing,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = RedStart,
                    disabledContainerColor = Color.White.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("red_packet_grab_button")
            ) {
                Text(
                    text = when {
                        grabbing -> "领取中..."
                        packet.my_claim != null -> "已领取 ${packet.my_claim.amount} 积分"
                        packet.status == "expired" -> "红包已过期"
                        packet.remain_count <= 0 || packet.status == "finished" -> "红包已抢完"
                        else -> "抢红包"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

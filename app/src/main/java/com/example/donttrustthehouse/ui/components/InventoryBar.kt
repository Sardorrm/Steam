package com.example.donttrustthehouse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.donttrustthehouse.model.GameItem
import com.example.donttrustthehouse.model.ItemType

@Composable
fun InventoryBar(
    inventory: List<GameItem>,
    selectedItem: GameItem?,
    onSelectItem: (GameItem?) -> Unit,
    onUseItem: (GameItem) -> Unit,
    onDropItem: (GameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (inventory.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xCC18121E),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x44CE93D8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("inventory_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INV (${inventory.size})",
                        color = Color(0xFFCE93D8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(inventory, key = { it.id }) { item ->
                            InventoryItemChip(
                                item = item,
                                onClick = { onSelectItem(item) }
                            )
                        }
                    }
                }
            }
        }

        // Detailed Item Inspection Modal Dialog
        selectedItem?.let { item ->
            ItemInspectionDialog(
                item = item,
                onDismiss = { onSelectItem(null) },
                onUse = {
                    onUseItem(item)
                    onSelectItem(null)
                },
                onDrop = {
                    onDropItem(item)
                    onSelectItem(null)
                }
            )
        }
    }
}

@Composable
private fun InventoryItemChip(
    item: GameItem,
    onClick: () -> Unit
) {
    val (icon, color) = getItemVisuals(item.type)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x992B1C33),
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("inventory_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.name,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ItemInspectionDialog(
    item: GameItem,
    onDismiss: () -> Unit,
    onUse: () -> Unit,
    onDrop: () -> Unit
) {
    val (icon, color) = getItemVisuals(item.type)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1B1422),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .testTag("item_inspection_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.name,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Item Type Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color.copy(alpha = 0.25f),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = item.type.name,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description card
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261D30)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.description,
                        color = Color(0xFFE0E0E0),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = if (item.type == ItemType.NOTE) FontFamily.Monospace else FontFamily.Default
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDrop,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_drop_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Drop", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Drop", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onUse,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_use_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(
                            imageVector = when (item.type) {
                                ItemType.KEY -> Icons.Default.LockOpen
                                ItemType.NOTE -> Icons.AutoMirrored.Filled.MenuBook
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = "Use",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.type == ItemType.NOTE) "Read" else "Use",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close dialog",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusNotificationBanner(
    notification: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xF0181F26),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF80D8FF), RoundedCornerShape(10.dp))
                .clickable(onClick = onDismiss)
                .testTag("status_notification_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Status",
                    tint = Color(0xFF80D8FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = notification ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun getItemVisuals(type: ItemType): Pair<ImageVector, Color> {
    return when (type) {
        ItemType.KEY -> Icons.Default.Key to Color(0xFFFFD54F)
        ItemType.SEDATIVE -> Icons.Default.Healing to Color(0xFF00E5FF)
        ItemType.BATTERY -> Icons.Default.BatteryChargingFull to Color(0xFFFFEB3B)
        ItemType.TALISMAN -> Icons.Default.Security to Color(0xFFE040FB)
        ItemType.NOTE -> Icons.AutoMirrored.Filled.MenuBook to Color(0xFFFFCC80)
    }
}

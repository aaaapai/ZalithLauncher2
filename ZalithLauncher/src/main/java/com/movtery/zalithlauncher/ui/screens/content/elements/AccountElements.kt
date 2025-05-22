package com.movtery.zalithlauncher.ui.screens.content.elements

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountType
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.isSkinChangeAllowed
import com.movtery.zalithlauncher.game.account.otherserver.models.AuthResult
import com.movtery.zalithlauncher.game.account.otherserver.models.Servers.Server
import com.movtery.zalithlauncher.game.skin.SkinModelType
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.path.UrlManager
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import java.io.IOException
import java.nio.file.Files
import java.util.regex.Pattern

/**
 * 微软登录的操作状态
 */
sealed interface MicrosoftLoginOperation {
    data object None : MicrosoftLoginOperation
    /** 微软账号相关提示Dialog流程 */
    data object Tip : MicrosoftLoginOperation
    /** 正式开始登陆微软账号流程 */
    data object RunTask: MicrosoftLoginOperation
}

/**
 * 离线登陆的操作状态
 */
sealed interface LocalLoginOperation {
    data object None : LocalLoginOperation
    /** 编辑用户名流程 */
    data object Edit : LocalLoginOperation
    /** 创建账号流程 */
    data class Create(val userName: String) : LocalLoginOperation
    /** 警告非法用户名流程 */
    data class Alert(val userName: String) : LocalLoginOperation
}

/**
 * 添加认证服务器时的状态
 */
sealed interface ServerOperation {
    data object None : ServerOperation
    data object AddNew : ServerOperation
    data class Delete(val serverName: String, val serverIndex: Int) : ServerOperation
    data class Add(val serverUrl: String) : ServerOperation
    data class OnThrowable(val throwable: Throwable) : ServerOperation
}

/**
 * 账号操作的状态
 */
sealed interface AccountOperation {
    data object None : AccountOperation
    data class Delete(val account: Account) : AccountOperation
    data class Refresh(val account: Account) : AccountOperation
    data class OnFailed(val th: Throwable) : AccountOperation
}

/**
 * 更换账号皮肤的状态
 */
sealed interface AccountSkinOperation {
    data object None: AccountSkinOperation
    /** 保存皮肤文件 */
    data class SaveSkin(val uri: Uri): AccountSkinOperation
    /** 选择皮肤模型，便于保存皮肤时，顺便将模型类型写入账号文件 */
    data class SelectSkinModel(val uri: Uri): AccountSkinOperation
    /** 警告皮肤模型的一些注意事项 */
    data object AlertModel: AccountSkinOperation
    /** 警告用户是否真的想重置皮肤 */
    data object PreResetSkin: AccountSkinOperation
    /** 重置皮肤（清除皮肤并刷新账号皮肤模型为""） */
    data object ResetSkin: AccountSkinOperation
}

/**
 * 认证服务器登陆时的状态
 */
sealed interface OtherLoginOperation {
    data object None : OtherLoginOperation
    /** 账号登陆（输入账号密码Dialog）流程 */
    data class OnLogin(val server: Server) : OtherLoginOperation
    /** 登陆失败流程 */
    data class OnFailed(val th: Throwable) : OtherLoginOperation
    /** 账号存在多角色的情况，多角色处理流程 */
    data class SelectRole(
        val profiles: List<AuthResult.AvailableProfiles>,
        val selected: (AuthResult.AvailableProfiles) -> Unit
    ) : OtherLoginOperation
}

@Composable
fun AccountAvatar(
    modifier: Modifier = Modifier,
    avatarSize: Int = 64,
    account: Account?,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(all = 12.dp)
        ) {
            if (account != null) {
                PlayerFace(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    account = account,
                    avatarSize = avatarSize
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterHorizontally),
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = contentColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = account?.username ?: stringResource(R.string.account_add_new_account),
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor
            )
            if (account != null) {
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = getAccountTypeName(context, account),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun PlayerFace(
    modifier: Modifier = Modifier,
    account: Account,
    avatarSize: Int = 64
) {
    val context = LocalContext.current
    val avatarBitmap = remember(account) {
        getAvatarFromAccount(context, account, avatarSize).asImageBitmap()
    }

    val newAvatarSize = avatarBitmap.width.dp

    Image(
        modifier = modifier.size(newAvatarSize),
        bitmap = avatarBitmap,
        contentDescription = null
    )
}

@Composable
fun AccountItem(
    modifier: Modifier = Modifier,
    currentAccount: Account?,
    account: Account,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onSelected: (uniqueUUID: String) -> Unit = {},
    onChangeSkin: () -> Unit = {},
    onResetSkin: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val selected = currentAccount?.uniqueUUID == account.uniqueUUID
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        onClick = {
            if (selected) return@Surface
            onSelected(account.uniqueUUID)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = MaterialTheme.shapes.large)
                .padding(all = 8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = {
                    if (selected) return@RadioButton
                    onSelected(account.uniqueUUID)
                }
            )
            PlayerFace(
                modifier = Modifier.align(Alignment.CenterVertically),
                account = account,
                avatarSize = 46
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            ) {
                val context = LocalContext.current
                Text(text = account.username)
                Text(
                    text = getAccountTypeName(context, account),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Row {
                val isLocalHasSkin = account.isLocalAccount() && account.hasSkinFile
                val icon = if (isLocalHasSkin) Icons.Default.RestartAlt else Icons.Outlined.Checkroom
                val description = if (isLocalHasSkin) {
                    stringResource(R.string.generic_reset)
                } else {
                    stringResource(R.string.account_change_skin)
                }
                val onClickAction = if (isLocalHasSkin) onResetSkin else onChangeSkin

                IconButton(
                    onClick = onClickAction,
                    enabled = account.isSkinChangeAllowed()
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = icon,
                        contentDescription = description
                    )
                }
                IconButton(
                    onClick = onRefreshClick,
                    enabled = account.accountType != AccountType.LOCAL.tag
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.generic_refresh)
                    )
                }
                IconButton(
                    onClick = onDeleteClick
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.generic_delete)
                    )
                }
            }
        }
    }
}

@Composable
fun LoginItem(
    modifier: Modifier = Modifier,
    serverName: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 4.dp, vertical = 12.dp))
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.Default.Add,
            contentDescription = serverName
        )
        Spacer(
            modifier = Modifier.width(8.dp)
        )
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = serverName,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun ServerItem(
    modifier: Modifier = Modifier,
    server: Server,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(start = 4.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = server.serverName,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(
            modifier = Modifier.width(8.dp)
        )
        IconButton(
            onClick = onDeleteClick
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.generic_delete)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MicrosoftLoginTipDialog(
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    val context = LocalContext.current
    SimpleAlertDialog(
        title = stringResource(R.string.account_supporting_microsoft_tip_title),
        text = {
            Text(
                text = stringResource(R.string.account_supporting_microsoft_tip_link_text),
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow {
                IconTextButton(
                    onClick = {
                        NetWorkUtils.openLink(context, UrlManager.URL_MINECRAFT_PURCHASE)
                    },
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    text = stringResource(R.string.account_supporting_microsoft_tip_link_purchase)
                )
                IconTextButton(
                    onClick = {
                        NetWorkUtils.openLink(context, "https://www.minecraft.net/msaprofile/mygames/editprofile")
                    },
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    text = stringResource(R.string.account_supporting_microsoft_tip_link_make_gameid)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.account_supporting_microsoft_tip_hint_t1),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t2))
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t3, InfoDistributor.LAUNCHER_NAME))
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t4))
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t5))
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t6))
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t7))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.account_supporting_microsoft_tip_hint_t8))
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmText = stringResource(R.string.account_login),
        onConfirm = onConfirm,
        onCancel = onDismissRequest,
        onDismissRequest = onDismissRequest
    )
}

private val localNamePattern = Pattern.compile("[^a-zA-Z0-9_]")

@Composable
fun LocalLoginDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (isUserNameInvalid: Boolean, userName: String) -> Unit
) {
    var userName by rememberSaveable { mutableStateOf("") }
    var isUserNameInvalid by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    SimpleEditDialog(
        title = stringResource(R.string.account_local_create_account),
        value = userName,
        onValueChange = { userName = it.trim() },
        label = { Text(text = stringResource(R.string.account_label_username)) },
        isError = isUserNameInvalid,
        supportingText = {
            val errorText = when {
                userName.isEmpty() -> stringResource(R.string.account_supporting_username_invalid_empty)
                userName.length <= 2 -> stringResource(R.string.account_supporting_username_invalid_short)
                userName.length > 16 -> stringResource(R.string.account_supporting_username_invalid_long)
                localNamePattern.matcher(userName).find() -> stringResource(R.string.account_supporting_username_invalid_illegal_characters)
                else -> ""
            }.also {
                isUserNameInvalid = it.isNotEmpty()
            }
            if (isUserNameInvalid) {
                Text(text = errorText)
            }
        },
        singleLine = true,
        extraContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                IconTextButton(
                    onClick = {
                        NetWorkUtils.openLink(context, UrlManager.URL_MINECRAFT_PURCHASE)
                    },
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    text = stringResource(R.string.account_supporting_microsoft_tip_link_purchase)
                )
            }
        },
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (userName.isNotEmpty()) {
                onConfirm(isUserNameInvalid, userName)
            }
        }
    )
}

@Composable
fun OtherServerLoginDialog(
    server: Server,
    onRegisterClick: (url: String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onConfirm: (email: String, password: String) -> Unit = { _, _ -> }
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val confirmAction = { //确认操作
        if (email.isNotEmpty() && password.isNotEmpty()) {
            onConfirm(email, password)
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = server.serverName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.size(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val passwordFocus = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = email,
                        onValueChange = { email = it },
                        isError = email.isEmpty(),
                        label = { Text(text = stringResource(R.string.account_label_email)) },
                        supportingText = {
                            if (email.isEmpty()) {
                                Text(text = stringResource(R.string.account_supporting_email_invalid_empty))
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                //自动跳到密码输入框，无缝衔接
                                passwordFocus.requestFocus()
                            }
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus),
                        value = password,
                        onValueChange = { password = it },
                        isError = password.isEmpty(),
                        label = { Text(text = stringResource(R.string.account_label_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Transparent,
                        ),
                        supportingText = {
                            if (password.isEmpty()) {
                                Text(text = stringResource(R.string.account_supporting_password_invalid_empty))
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                //用户按下返回，甚至可以在这里直接进行登陆
                                focusManager.clearFocus(true)
                                confirmAction()
                            }
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                    if (!server.register.isNullOrEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            IconTextButton(
                                onClick = {
                                    onRegisterClick(server.register!!)
                                },
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                text = stringResource(R.string.account_other_login_register)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest
                    ) {
                        Text(text = stringResource(R.string.generic_cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = confirmAction
                    ) {
                        Text(text = stringResource(R.string.generic_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun SelectSkinModelDialog(
    onDismissRequest: () -> Unit = {},
    onSelected: (SkinModelType) -> Unit = {}
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.account_change_skin_select_model_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.size(16.dp))

                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.account_change_skin_select_model_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onSelected(SkinModelType.STEVE)
                        }
                    ) {
                        Text(text = stringResource(R.string.account_change_skin_model_steve))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onSelected(SkinModelType.ALEX)
                        }
                    ) {
                        Text(text = stringResource(R.string.account_change_skin_model_alex))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDismissRequest
                    ) {
                        Text(text = stringResource(R.string.generic_cancel))
                    }
                }
            }
        }
    }
}

@Throws(Exception::class)
private fun getAvatarFromAccount(context: Context, account: Account, size: Int): Bitmap {
    val skin = account.getSkinFile()
    if (skin.exists()) {
        runCatching {
            Files.newInputStream(skin.toPath()).use { `is` ->
                val bitmap = BitmapFactory.decodeStream(`is`)
                    ?: throw IOException("Failed to read the skin picture and try to parse it to a bitmap")
                return getAvatar(bitmap, size)
            }
        }.onFailure { e ->
            Log.e("SkinLoader", "Failed to load avatar from locally!", e)
        }
    }
    return getDefaultAvatar(context, size)
}

@Throws(Exception::class)
private fun getDefaultAvatar(context: Context, size: Int): Bitmap {
    val `is` = context.assets.open("steve.png")
    return getAvatar(BitmapFactory.decodeStream(`is`), size)
}

private fun getAvatar(skin: Bitmap, size: Int): Bitmap {
    val faceOffset = Math.round(size / 18.0).toFloat()
    val scaleFactor = skin.width / 64.0f
    val faceSize = Math.round(8 * scaleFactor)
    val faceBitmap = Bitmap.createBitmap(skin, faceSize, faceSize, faceSize, faceSize, null as Matrix?, false)
    val hatBitmap = Bitmap.createBitmap(skin, Math.round(40 * scaleFactor), faceSize, faceSize, faceSize, null as Matrix?, false)
    val avatar = createBitmap(size, size)
    val canvas = android.graphics.Canvas(avatar)
    val faceScale = ((size - 2 * faceOffset) / faceSize)
    val hatScale = (size.toFloat() / faceSize)
    var matrix = Matrix()
    matrix.postScale(faceScale, faceScale)
    val newFaceBitmap = Bitmap.createBitmap(faceBitmap, 0, 0, faceSize, faceSize, matrix, false)
    matrix = Matrix()
    matrix.postScale(hatScale, hatScale)
    val newHatBitmap = Bitmap.createBitmap(hatBitmap, 0, 0, faceSize, faceSize, matrix, false)
    canvas.drawBitmap(newFaceBitmap, faceOffset, faceOffset, Paint(Paint.ANTI_ALIAS_FLAG))
    canvas.drawBitmap(newHatBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
    return avatar
}
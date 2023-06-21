package de.jackBeBack.ui.composables

import android.content.Context
import android.graphics.Outline
import android.os.Build
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.WindowCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*
import android.view.*
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import de.jackBeBack.R
import java.util.*
import kotlin.text.Typography

/**
 * Contains the default values used by [ShareBottomSheetDialog].
 */
object BottomSheetDialogDefaults {
    val HandlerWidth: Dp = 30.dp
    val HandlerHeight: Dp = 5.dp
    val HandlerShape: Shape = RoundedCornerShape(5.dp)
    const val HandlerColorOpacity = 0.4f

    val BottomSheetDialogLayoutShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val BottomSheetDialogLayoutContentPadding =
        PaddingValues(start = 16.dp, bottom = 12.dp, end = 16.dp)
    val BottomSheetDialogMaxWidthDp = 640.dp
    val BottomSheetDialogTopHeight = 70.dp
    val BottomSheetDialogTopTitlePadding = 12.dp
    const val TitleMaxLines = 2
}

/**
 * <a href="https://www.figma.com/file/8b41rXlBwgauSS1ktORogr/Share-Design-System" class="external" target="_blank">Design System 2.0 ShareBottomSheetDialog</a>.
 * Bottom sheets display content and actions that complement the screen’s primary content.
 * A typical example is the bottom sheet menu, containing a list of options.
 * There are two types of bottom sheets: standard and modal.
 * Modal bottom sheets are above a scrim (dimmer of background) while standard bottom sheets don't have a scrim.
 *
 * ![Share Bottom Sheet Dialog image](https://www.figma.com/file/8b41rXlBwgauSS1ktORogr/Share-Design-System-2.0?node-id=5298%3A65854&t=voaxF7a8s3sJiYff-1)
 *
 * The dialog is visible as long as it is part of the composition hierarchy.
 * In order to let the user dismiss the BottomSheetDialog, the implementation of [onDismissRequest] should
 * contain a way to remove to remove the dialog from the composition hierarchy.
 *
 * Example usage:
 * @param isDarkMode is used to resolve the ShareBottomSheetDialog's color in different states of Mode(Dark, Light).
 * If it is not used, the color themes are set according to the system setting of the Mode(Dark, Light).
 * @param titleText is present on the title of the [ShareBottomSheetDialog]
 * @param showTitle defines if the [titleText] is shown or not
 * @param onDismissRequest Executes when the user tries to dismiss the dialog.
 * @param onCloseButtonClick Executes when the user clicks close button.
 * @param properties [ShareBottomSheetDialogProperties] for further customization of this dialog's behavior.
 * @param content The content to be displayed inside the dialog.
 */
@Composable
fun BottomSheetDialog(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    titleText: String,
    showTitle: Boolean = true,
    onDismissRequest: () -> Unit,
    onCloseButtonClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val dialogId = rememberSaveable { UUID.randomUUID() }

    val dialog = remember(view, density) {
        BottomSheetDialogWrapper(
            context,
            onDismissRequest = onDismissRequest,
            composeView = view,
            density = density,
            dialogId = dialogId
        ).apply {
            setContent(composition) {
                BottomSheetDialogLayout(
                    modifier = Modifier
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .semantics { dialog() },
                    titleText = titleText,
                    showTitle = showTitle,
                    onCloseButtonClick = onCloseButtonClick,
                    content = {
                        currentContent()
                    }
                )
            }
        }
    }

    DisposableEffect(dialog) {
        dialog.show()
        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }
}


/**
 * ![Share Bottom Sheet Dialog Top image](https://www.figma.com/file/8b41rXlBwgauSS1ktORogr/Share-Design-System-2.0?node-id=5235%3A65399&t=Ullst9yiRuFWPjTF-0)
 */
@Composable
private fun BottomSheetDialogTop(
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    showTitle: Boolean = true,
    titleText: String,
    titleStyle: TextStyle = TextStyle.Default,
    handlerColor: Color = Color.DarkGray,
    contentColor: Color = Color.DarkGray,
    onCloseButtonClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(BottomSheetDialogDefaults.BottomSheetDialogTopHeight)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.1f)
        ) {
            if (isFullScreen) {
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = onCloseButtonClick
                ){
                    Text(text = "X")
                }
            }
        }

        Spacer(modifier = Modifier.width(BottomSheetDialogDefaults.BottomSheetDialogTopTitlePadding))

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.8f)
        ) {
            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(
                                width = BottomSheetDialogDefaults.HandlerWidth,
                                height = BottomSheetDialogDefaults.HandlerHeight
                            )
                            .clip(BottomSheetDialogDefaults.HandlerShape)
                            .background(handlerColor)
                    )
                }
            }

            if (showTitle && titleText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = titleText,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = BottomSheetDialogDefaults.TitleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        style = titleStyle
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(BottomSheetDialogDefaults.BottomSheetDialogTopTitlePadding))

        Box(
            modifier = if (showTitle && titleText.isNotEmpty()) {
                Modifier
                    .fillMaxHeight(0.6f)
                    .align(Alignment.Bottom)
            } else {
                Modifier
                    .fillMaxHeight()
            }
                .weight(0.1f)
        ) {
            if (!isFullScreen) {
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = onCloseButtonClick
                ){
                    Text(text = "X")
                }
            }
        }
    }
}

@Composable
private fun BottomSheetDialogLayout(
    modifier: Modifier = Modifier,
    shape: Shape = BottomSheetDialogDefaults.BottomSheetDialogLayoutShape,
    backgroundColor: Color = Color.White,
    contentPadding: PaddingValues = BottomSheetDialogDefaults.BottomSheetDialogLayoutContentPadding,
    titleText: String,
    showTitle: Boolean = true,
    onCloseButtonClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var width by remember { mutableStateOf(1) }
    var height by remember { mutableStateOf(1) }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val isFullScreen = screenHeightDp <= height.pxToDp() && screenWidthDp <= width.pxToDp()

    val updatedShape = if (isFullScreen) RoundedCornerShape(0.dp) else shape

    Layout(
        content = {
            Surface(shape = updatedShape, color = backgroundColor) {
                Column(
                    modifier = Modifier
                        .padding(contentPadding)
                ) {
                    BottomSheetDialogTop(
                        modifier = modifier.verticalScroll(rememberScrollState()),
                        isFullScreen = isFullScreen,
                        titleText = titleText,
                        showTitle = showTitle,
                        onCloseButtonClick = onCloseButtonClick
                    )
                    content()
                }
            }
        },
        modifier = modifier
    ) { measurable, constraints ->
        val placeable = measurable.map { it.measure(constraints) }
        width = placeable.maxByOrNull { it.width }?.width ?: constraints.minWidth
        height = placeable.maxByOrNull { it.height }?.height ?: constraints.minHeight
        layout(width, height) {
            placeable.forEach { it.placeRelative(0, 0) }
        }
    }
}
class BottomSheetDialogWrapper(
    context: Context,
    private var onDismissRequest: () -> Unit,
    private val composeView: View,
    density: Density,
    dialogId: UUID
) : BottomSheetDialog(
    context
), ViewRootForInspector {
    private val bottomSheetDialogLayout: BottomSheetDialogLayout

    private val bottomSheetCallbackForAnimation: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    onDismissRequest()
                }
            }
        }

    private val maxSupportedElevation = 30.dp

    override val subCompositionView: AbstractComposeView get() = bottomSheetDialogLayout

    // to control insets
    private val windowInsetsController = window?.let {
        WindowCompat.getInsetsController(it, it.decorView)
    }

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        bottomSheetDialogLayout = BottomSheetDialogLayout(context, window).apply {
            // Set unique id for AbstractComposeView. This allows state restoration for the state
            // defined inside the Dialog via rememberSaveable()
            setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")
            // Enable children to draw their shadow by not clipping them
            clipChildren = false
            // Allocate space for elevation
            with(density) { elevation = maxSupportedElevation.toPx() }
            // Simple outline to force window manager to allocate space for shadow.
            // Note that the outline affects clickable area for the dismiss listener. In case of
            // shapes like circle the area for dismiss might be to small (rectangular outline
            // consuming clicks outside of the circle).
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, result: Outline) {
                    result.setRect(0, 0, view.width, view.height)
                    // We set alpha to 0 to hide the view's shadow and let the composable to draw
                    // its own shadow. This still enables us to get the extra space needed in the
                    // surface.
                    result.alpha = 0f
                }
            }
        }

        /**
         * Disables clipping for [this] and all its descendant [ViewGroup]s until we reach a
         * [BottomSheetDialogLayout] (the [ViewGroup] containing the Compose hierarchy).
         */
        fun ViewGroup.disableClipping() {
            clipChildren = false
            if (this is BottomSheetDialogLayout) return
            for (i in 0 until childCount) {
                (getChildAt(i) as? ViewGroup)?.disableClipping()
            }
        }

        // Turn of all clipping so shadows can be drawn outside the window
        (window.decorView as? ViewGroup)?.disableClipping()
        setContentView(bottomSheetDialogLayout)
        bottomSheetDialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        bottomSheetDialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        bottomSheetDialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    cancel()
                }
            })
    }

    fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
        bottomSheetDialogLayout.setContent(parentComposition, children)
    }

    override fun setDismissWithAnimation(dismissWithAnimation: Boolean) {
        super.setDismissWithAnimation(dismissWithAnimation)
        if (dismissWithAnimation) {
            behavior.addBottomSheetCallback(bottomSheetCallbackForAnimation)
        } else {
            behavior.removeBottomSheetCallback(bottomSheetCallbackForAnimation)
        }
    }

    fun disposeComposition() {
        bottomSheetDialogLayout.disposeComposition()
    }

    override fun cancel() {
        super.cancel()
        onDismissRequest()
    }
}

/**
 * Provides the underlying window of a [ShareBottomSheetDialogLayout].
 */
interface DialogWindowProvider {
    val window: Window
}

@Suppress("ViewConstructor")
class BottomSheetDialogLayout(
    context: Context, override val window: Window
) : AbstractComposeView(context), DialogWindowProvider {
    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }
}



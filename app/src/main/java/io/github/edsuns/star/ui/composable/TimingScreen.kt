package io.github.edsuns.star.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.github.edsuns.chaoxing.model.Timing
import io.github.edsuns.star.R
import io.github.edsuns.star.Repository
import io.github.edsuns.star.ui.MainViewModel
import io.github.edsuns.star.util.Result
import io.github.edsuns.star.util.produceUiState
import kotlinx.coroutines.launch
import java.io.IOException


/**
 * Created by Edsuns@qq.com on 2021/6/27.
 */

@Composable
fun TimingScreen(
    onLogoutClicked: (LoginEvent) -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    val (postUiState, refreshPost, clearError) = produceUiState(Repository) {
        try {
            fetchAllActiveTiming()
        } catch (err: IOException) {
            Result.Error(err)
        }
    }

    val data = postUiState.value.data
    val hasData = data != null && data.isNotEmpty()
    val hasErr = postUiState.value.hasError
    if (hasErr) {
        val errorMessage = stringResource(id = R.string.network_error)
        val retryMessage = stringResource(id = R.string.retry)

        // If onRefreshPosts or onErrorDismiss change while the LaunchedEffect is running,
        // don't restart the effect and use the latest lambda values.
        val onRefreshPostsState by rememberUpdatedState(refreshPost)
        val onErrorDismissState by rememberUpdatedState(clearError)

        // Show snackbar using a coroutine, when the coroutine is cancelled the snackbar will
        // automatically dismiss. This coroutine will cancel whenever posts.hasError is false
        // (thanks to the surrounding if statement) or if scaffoldState.snackbarHostState changes.
        LaunchedEffect(scaffoldState.snackbarHostState) {
            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = retryMessage
            )
            when (snackbarResult) {
                SnackbarResult.ActionPerformed -> onRefreshPostsState()
                SnackbarResult.Dismissed -> onErrorDismissState()
            }
        }
    }

    val viewModel = viewModel(MainViewModel::class.java)

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            MainTopAppBar(
                topAppBarText = stringResource(id = R.string.app_name),
                onInfoClicked = { viewModel.onInfoClicked() },
                onLogoutClicked = { onLogoutClicked(LoginEvent.NavigateBack) }
            )
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        LoadingContent(
            empty = !hasData,
            emptyContent = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (postUiState.value.loading) {
                        CircularProgressIndicator()
                    } else {
                        // if there are no posts, and no error, let the user refresh manually
                        TextButton(onClick = refreshPost, Modifier.fillMaxSize()) {
                            Text(stringResource(R.string.tap_to_load), textAlign = TextAlign.Center)
                        }
                    }
                }
            },
            loading = postUiState.value.loading,
            onRefresh = refreshPost,
            content = {
                ActiveTimingList(
                    onRefresh = refreshPost, data = data!!, modifier = modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
fun ActiveTimingList(
    onRefresh: () -> Unit,
    data: List<Timing>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(modifier = modifier) {
        items(data.size) { index ->
            val timing = data[index]
            val onClickHandle: () -> Unit = {
                coroutineScope.launch {
                    if (Repository.onTimingClicked(timing)) {
                        // TODO modify timing data directly instead of refresh
                        onRefresh()
                    }
                }
            }
            val optionSelected = timing.state == Timing.State.SUCCESS
            val answerBackgroundColor = if (optionSelected) {
                MaterialTheme.colors.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colors.background
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = optionSelected,
                        onClick = onClickHandle
                    )
                    .background(answerBackgroundColor)
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = timing.type.description,
                        color = MaterialTheme.colors.primary,
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = timing.course.name,
                        style = MaterialTheme.typography.subtitle2
                    )
                }
                CheckboxIconButton(
                    isChecked = optionSelected,
                    onClick = onClickHandle
                )
            }
            PostListDivider()
        }
    }
}

/**
 * Full-width divider with padding for [PostList]
 */
@Composable
private fun PostListDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
    )
}

/**
 * Display an initial empty state or swipe to refresh content.
 *
 * @param empty (state) when true, display [emptyContent]
 * @param emptyContent (slot) the content to display for the empty state
 * @param loading (state) when true, display a loading spinner over [content]
 * @param onRefresh (event) event to request refresh
 * @param content (slot) the main content to show
 */
@Composable
private fun LoadingContent(
    empty: Boolean,
    emptyContent: @Composable () -> Unit,
    loading: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    if (empty) {
        emptyContent()
    } else {
        SwipeRefresh(
            state = rememberSwipeRefreshState(loading),
            onRefresh = onRefresh,
            content = content,
        )
    }
}

@Composable
fun MainTopAppBar(topAppBarText: String, onInfoClicked: () -> Unit, onLogoutClicked: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = topAppBarText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        },
        navigationIcon = {
            IconButton(onClick = onInfoClicked, modifier = Modifier.width(68.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(id = R.string.about),
                    modifier = Modifier.alpha(0.7f)
                )
            }
        },
        actions = {
            IconButton(onClick = onLogoutClicked, modifier = Modifier.width(68.dp)) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = stringResource(id = R.string.logout)
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    )
}
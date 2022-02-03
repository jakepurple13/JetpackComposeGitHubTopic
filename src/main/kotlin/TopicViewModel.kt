import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

class TopicViewModel {

    var text by mutableStateOf("")
    var topicList by mutableStateOf(emptyList<GitHubTopic>())
    val historyTopicList = mutableStateListOf<GitHubTopic>()
    val topicsToSearch = mutableStateListOf<String>()

    var page by mutableStateOf(1)

    var repoSelected by mutableStateOf(-1)
    var topicSelected by mutableStateOf(-1)
    var historySelected by mutableStateOf(-1)

    var showSearching by mutableStateOf(false)
    var isAskingToClose by mutableStateOf(false)

    fun onTopicAdd() {
        if (text.isNotEmpty() && text !in topicsToSearch) {
            topicsToSearch.add(text.trim())
            text = ""
        }
    }

    fun addTopic(topic: String) {
        topicsToSearch.add(topic)
    }

    fun removeTopic(topic: String) {
        topicsToSearch.remove(topic)
    }

    fun toggleTopic(topic: String) {
        if (topic in topicsToSearch) {
            removeTopic(topic)
        } else {
            addTopic(topic)
        }
    }

    suspend fun refresh(state: LazyListState) = search(state, page)

    suspend fun search(state: LazyListState, pageToSearch: Int = page) {
        if (topicsToSearch.isNotEmpty()) {
            page = pageToSearch
            repoSelected = -1
            withContext(Dispatchers.IO) {
                topicList = getTopics2(
                    searching = { showSearching = true },
                    done = { showSearching = false },
                    topics = topicsToSearch.toTypedArray(),
                    page = pageToSearch
                )
            }
            state.scrollToItem(0)
        } else {
            isAskingToClose = true
        }
    }

    suspend fun previousPage(state: LazyListState) {
        if (topicsToSearch.isNotEmpty()) {
            if (page > 1) {
                repoSelected = -1
                withContext(Dispatchers.IO) {
                    topicList = getTopics2(
                        searching = { showSearching = true },
                        done = { showSearching = false },
                        topics = topicsToSearch.toTypedArray(),
                        page = --page
                    )
                }
                state.scrollToItem(0)
            }
        } else {
            isAskingToClose = true
        }
    }

    suspend fun nextPage(state: LazyListState) {
        if (topicsToSearch.isNotEmpty()) {
            repoSelected = -1
            withContext(Dispatchers.IO) {
                topicList = getTopics2(
                    searching = { showSearching = true },
                    done = { showSearching = false },
                    topics = topicsToSearch.toTypedArray(),
                    page = ++page
                )
            }
            state.scrollToItem(0)
        } else {
            isAskingToClose = true
        }
    }

    fun cardClick(item: GitHubTopic) {
        Desktop.getDesktop().browse(URI.create(item.url))
        if (item !in historyTopicList) historyTopicList.add(item)
    }

    fun historyClick(item: GitHubTopic) {
        Desktop.getDesktop().browse(URI.create(item.url))
    }

    suspend fun previousTopicSelect(topicState: LazyListState) {
        if (topicsToSearch.isNotEmpty() && topicSelected > -1) {
            topicState.animateScrollToItem(--topicSelected)
        }
    }

    suspend fun nextTopicSelect(topicState: LazyListState) {
        if (topicsToSearch.isNotEmpty() && topicSelected < topicsToSearch.lastIndex) {
            topicState.animateScrollToItem(++topicSelected)
        }
    }

    suspend fun scrollToTopTopic(topicState: LazyListState) {
        if (topicsToSearch.isNotEmpty()) {
            topicSelected = 0
            topicState.animateScrollToItem(0)
        }
    }

    suspend fun scrollToBottomTopic(topicState: LazyListState) {
        if (topicsToSearch.isNotEmpty()) {
            topicSelected = topicsToSearch.lastIndex
            topicState.animateScrollToItem(topicSelected)
        }
    }

    fun deleteTopic() {
        if (topicsToSearch.isNotEmpty() && topicSelected in 0..topicsToSearch.lastIndex)
            topicsToSearch.removeAt(topicSelected)
    }

    suspend fun previousRepoSelect(repoState: LazyListState) {
        if (topicList.isNotEmpty() && repoSelected > -1) {
            repoState.animateScrollToItem(--repoSelected)
        }
    }

    suspend fun nextRepoSelect(repoState: LazyListState) {
        if (topicList.isNotEmpty() && repoSelected < topicList.lastIndex) {
            repoState.animateScrollToItem(++repoSelected)
        }
    }

    suspend fun scrollToTopRepo(repoState: LazyListState) {
        if (topicList.isNotEmpty()) {
            repoSelected = 0
            repoState.animateScrollToItem(0)
        }
    }

    suspend fun scrollToBottomRepo(repoState: LazyListState) {
        if (topicList.isNotEmpty()) {
            repoSelected = topicList.lastIndex
            repoState.animateScrollToItem(repoSelected)
        }
    }

    fun openSelectedRepo() {
        if (topicList.isNotEmpty() && repoSelected in 0..topicList.lastIndex) {
            topicList.getOrNull(repoSelected)?.let {
                val url = it.url
                if (url.isNotEmpty()) {
                    Desktop.getDesktop().browse(URI(url))
                }
                if (it !in historyTopicList) historyTopicList.add(it)
            }
        }
    }

    suspend fun previousHistorySelect(historyState: LazyListState) {
        if (historyTopicList.isNotEmpty() && historySelected > -1) {
            historyState.animateScrollToItem(--historySelected)
        }
    }

    suspend fun nextHistorySelect(historyState: LazyListState) {
        if (historyTopicList.isNotEmpty() && historySelected < historyTopicList.lastIndex) {
            historyState.animateScrollToItem(++historySelected)
        }
    }

    fun openSelectedHistory() {
        if (historyTopicList.isNotEmpty() && historySelected in 0..historyTopicList.lastIndex) {
            historyTopicList.getOrNull(historySelected)?.let {
                val url = it.url
                if (url.isNotEmpty()) {
                    Desktop.getDesktop().browse(URI(url))
                }
            }
        }
    }

    suspend fun scrollToTopHistory(historyState: LazyListState) {
        if (historyTopicList.isNotEmpty()) {
            historySelected = 0
            historyState.animateScrollToItem(0)
        }
    }

    suspend fun scrollToBottomHistory(historyState: LazyListState) {
        if (historyTopicList.isNotEmpty()) {
            historySelected = historyTopicList.lastIndex
            historyState.animateScrollToItem(historySelected)
        }
    }

}
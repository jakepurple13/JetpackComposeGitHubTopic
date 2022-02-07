import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Desktop
import java.net.URI
import java.time.Instant

class TopicViewModel {

    var text by mutableStateOf("")
    var repoList by mutableStateOf(emptyList<GitHubTopic>())
    var historyDBList by mutableStateOf<List<GitHubTopic>>(emptyList())
        private set
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
                repoList = getTopics2(
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
                    repoList = getTopics2(
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
                repoList = getTopics2(
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
        addTopicToHistory(item)
    }

    fun historyClick(item: GitHubTopic) {
        Desktop.getDesktop().browse(URI.create(item.url))
    }

    fun addSelectedTopicToHistory() {
        if (repoList.isNotEmpty()) {
            repoList.getOrNull(repoSelected)?.let { addTopicToHistory(it) }
        }
    }

    private fun Transaction.updateHistory() {
        registerInterceptor(object : StatementInterceptor {
            override fun afterCommit() {
                println("afterCommit")
                transaction(DbProperties.db) { historyDBList = TopicDao.all().mapToGitHubTopic() }
            }
        })
    }

    fun addTopicToHistory(item: GitHubTopic) {
        if (item !in historyDBList) {
            //historyTopicList.add(item)
            transaction(DbProperties.db) {
                updateHistory()
                TopicDao.new {
                    name = item.name
                    link = item.url
                    description = item.description
                    createdAt = Instant.parse(item.createdAt)
                    updatedAt = Instant.parse(item.updatedAt)
                    pushedAt = Instant.parse(item.pushedAt)
                    image = item.avatarUrl
                    topics = item.topics
                    watchers = item.watchers
                    stars = item.stars
                }
            }
        }
    }

    fun removeTopicFromHistory(item: GitHubTopic) {
        //historyTopicList.remove(item)
        transaction(DbProperties.db) {
            updateHistory()
            Topic.deleteWhere { Topic.link eq item.url }
        }
    }

    fun removeSelectedTopicFromHistory() {
        if (historyDBList.isNotEmpty() && historySelected in 0..historyDBList.lastIndex) {
            historyDBList.getOrNull(historySelected)?.let { removeTopicFromHistory(it) }
            if (historySelected == historyDBList.size) {
                historySelected = historyDBList.lastIndex
            }
        }
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
        if (topicsToSearch.isNotEmpty() && topicSelected in 0..topicsToSearch.lastIndex) {
            topicsToSearch.removeAt(topicSelected)
            if (topicSelected == topicsToSearch.size) {
                topicSelected = topicsToSearch.size - 1
            }
        }
    }

    suspend fun previousRepoSelect(repoState: LazyListState) {
        if (repoList.isNotEmpty() && repoSelected > -1) {
            repoState.animateScrollToItem(--repoSelected)
        }
    }

    suspend fun nextRepoSelect(repoState: LazyListState) {
        if (repoList.isNotEmpty() && repoSelected < repoList.lastIndex) {
            repoState.animateScrollToItem(++repoSelected)
        }
    }

    suspend fun scrollToTopRepo(repoState: LazyListState) {
        if (repoList.isNotEmpty()) {
            repoSelected = 0
            repoState.animateScrollToItem(0)
        }
    }

    suspend fun scrollToBottomRepo(repoState: LazyListState) {
        if (repoList.isNotEmpty()) {
            repoSelected = repoList.lastIndex
            repoState.animateScrollToItem(repoSelected)
        }
    }

    fun openSelectedRepo() {
        if (repoList.isNotEmpty() && repoSelected in 0..repoList.lastIndex) {
            repoList.getOrNull(repoSelected)?.let {
                val url = it.url
                if (url.isNotEmpty()) {
                    Desktop.getDesktop().browse(URI(url))
                }
                addTopicToHistory(it)
            }
        }
    }

    suspend fun previousHistorySelect(historyState: LazyListState) {
        if (historyDBList.isNotEmpty() && historySelected > -1) {
            historyState.animateScrollToItem(--historySelected)
        }
    }

    suspend fun nextHistorySelect(historyState: LazyListState) {
        if (historyDBList.isNotEmpty() && historySelected < historyDBList.lastIndex) {
            historyState.animateScrollToItem(++historySelected)
        }
    }

    fun openSelectedHistory() {
        if (historyDBList.isNotEmpty() && historySelected in 0..historyDBList.lastIndex) {
            historyDBList.getOrNull(historySelected)?.let {
                val url = it.url
                if (url.isNotEmpty()) {
                    Desktop.getDesktop().browse(URI(url))
                }
            }
        }
    }

    suspend fun scrollToTopHistory(historyState: LazyListState) {
        if (historyDBList.isNotEmpty()) {
            historySelected = 0
            historyState.animateScrollToItem(0)
        }
    }

    suspend fun scrollToBottomHistory(historyState: LazyListState) {
        if (historyDBList.isNotEmpty()) {
            historySelected = historyDBList.lastIndex
            historyState.animateScrollToItem(historySelected)
        }
    }

    init {
        transaction(DbProperties.db) { historyDBList = TopicDao.all().mapToGitHubTopic() }
    }

}
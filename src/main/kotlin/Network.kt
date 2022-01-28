import com.github.tsohr.JSONException
import com.github.tsohr.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubTopic(
    val url: String,
    val name: String,
    val description: String,
    val updatedAt: String,
    val pushedAt: String,
    val createdAt: String,
    val stars: Int,
    val watchers: Int,
    val avatarUrl: String? = null,
    val topics: List<String> = emptyList()
)

//typed this
// get github api topics
fun getTopics(searching: () -> Unit, done: () -> Unit, vararg topics: String): List<GitHubTopic> {
    searching()
    //added the .joinToString()
    val url = "https://api.github.com/search/repositories?q=" + topics.joinToString(separator = "+") { "topic:$it" } + "+sort:updated-desc"
    println(url)
    val request = URL(url).openConnection() as HttpURLConnection
    request.requestMethod = "GET"
    request.connect()
    val response = request.inputStream.bufferedReader().use { it.readText() }
    //println(response)
    //typed this
    //    #{url:.html_url, name:.name, description:.description, updated:.updated_at}
    //typed this
    //change string to json object
    val json = JSONObject(response)
    val items = json.getJSONArray("items").map {
        val item = it as JSONObject
        GitHubTopic(
            url = item.getString("html_url"),
            name = item.getString("name"),
            description = try {
                item.getString("description")
            } catch (e: JSONException) {
                "No description"
            },
            updatedAt = item.getString("updated_at"),
            pushedAt = item.getString("pushed_at"),
            createdAt = item.getString("created_at"),
            stars = item.getInt("stargazers_count"),
            watchers = item.getInt("watchers_count"),
        )
    }
    done()
    return items
}

//get github api topics page by page
fun getTopics2(searching: () -> Unit, done: () -> Unit, page: Int, vararg topics: String): List<GitHubTopic> {
    searching()
    val url = "https://api.github.com/search/repositories?q=" + topics.joinToString(separator = "+") { "topic:$it" } + "+sort:updated-desc&page=$page"
    println(url)
    val request = URL(url).openConnection() as HttpURLConnection
    request.requestMethod = "GET"
    request.connect()
    val response = request.inputStream.bufferedReader().use { it.readText() }
    //println(response)
    //    #{url:.html_url, name:.name, description:.description, updated:.updated_at}
    //avatar_url

    //change string to json object
    val json = JSONObject(response)
    val items = json.getJSONArray("items").map {
        val item = it as JSONObject
        GitHubTopic(
            url = item.getString("html_url"),
            name = item.getString("name"),
            description = try {
                item.getString("description")
            } catch (e: JSONException) {
                "No description"
            },
            updatedAt = item.getString("updated_at"),
            pushedAt = item.getString("pushed_at"),
            createdAt = item.getString("created_at"),
            stars = item.getInt("stargazers_count"),
            watchers = item.getInt("watchers_count"),
            avatarUrl = try {
                item
                    .getJSONObject("owner")
                    .getString("avatar_url")
            } catch (e: JSONException) {
                null
            },
            topics = try {
                item
                    .getJSONArray("topics")
                    .map { t -> t as String }
            } catch (e: JSONException) {
                emptyList()
            }
        )
    }
    done()
    return items
}

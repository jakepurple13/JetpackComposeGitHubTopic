/*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table.Dual.varchar
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll

val db by lazy { Database.connect("jdbc:h2:mem:test") }

*/
/*
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
 *//*


object Topic : IntIdTable() {
    val name = varchar("name", 50)
    val description = blob("description")
    val author = varchar("author", 50)
    val image = varchar("image", 50)
    val link = varchar("link", 50).uniqueIndex()
    val stars = integer("stars")
    val watchers = integer("watchers")
    val updatedAt = timestamp("updatedAt")
    val pushedAt = timestamp("pushedAt")
    val createdAt = timestamp("createdAt")
    val topics = blob("topics")
}

class TopicDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TopicDao>(Topic)

    var name by Topic.name
    var description by Topic.description
    var author by Topic.author
    var image by Topic.image
    var link by Topic.link
    var stars by Topic.stars
    var watchers by Topic.watchers
    var updatedAt by Topic.updatedAt
    var pushedAt by Topic.pushedAt
    var createdAt by Topic.createdAt
    var topics by Topic.topics
}
*/

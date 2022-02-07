import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun dbInit() {
    transaction(DbProperties.db) { SchemaUtils.create(Topic) }
}

object DbProperties {
    val db by lazy {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        Database.connect("jdbc:h2:./myh2file", "org.h2.Driver")
    }
}

/*data class GitHubTopic(
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
)*/

object Topic : IntIdTable() {
    val name = text("name")
    val description = text("description")
    val image = text("image").nullable()
    val link = text("link").uniqueIndex()
    val stars = integer("stars")
    val watchers = integer("watchers")
    val updatedAt = timestamp("updatedAt")
    val pushedAt = timestamp("pushedAt")
    val createdAt = timestamp("createdAt")
    val topics = text("topics")
}

class TopicDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TopicDao>(Topic) {
        const val SEPARATOR = "|||"
    }

    var name by Topic.name
    var description by Topic.description
    var image by Topic.image
    var link by Topic.link
    var stars by Topic.stars
    var watchers by Topic.watchers
    var updatedAt by Topic.updatedAt
    var pushedAt by Topic.pushedAt
    var createdAt by Topic.createdAt
    var topics by Topic.topics.transform(
        { it.joinToString(SEPARATOR) },
        { it.split(SEPARATOR) }
    )
}

fun SizedIterable<TopicDao>.mapToGitHubTopic() = map {
    GitHubTopic(
        name = it.name,
        url = it.link,
        createdAt = it.createdAt.toString(),
        updatedAt = it.updatedAt.toString(),
        pushedAt = it.pushedAt.toString(),
        description = it.description,
        topics = it.topics,
        stars = it.stars,
        watchers = it.watchers,
        avatarUrl = it.image,
    )
}

fun asdf() {
    val topics = transaction(DbProperties.db) {
        TopicDao.all().mapToGitHubTopic()
        registerInterceptor(object : StatementInterceptor {
            override fun afterCommit() {
                super.afterCommit()
            }
        })
    }
}
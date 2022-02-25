import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

fun dbInit() {
    println("Creating database...")
    transaction(DbProperties.db) { SchemaUtils.create(Topic) }
}

val FILE_SEPERATOR = File.separator

object DbProperties {
    val db by lazy {
        println("Connecting to database...")
        //Database.connect("jdbc:h2:./myh2file", "org.h2.Driver")
        //Database.connect("jdbc:h2:mem:regular", "org.h2.Driver")
        //Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
        //Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        //Database.connect("jdbc:sqlite:./data.db", "org.sqlite.JDBC")
        val f = "${System.getProperty("user.home")}${FILE_SEPERATOR}Desktop${FILE_SEPERATOR}githubtopics.db"
        Database.connect("jdbc:sqlite:$f", "org.sqlite.JDBC")
            //Database.connect("jdbc:h2:$f", "org.h2.Driver")
            .also { TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE }
    }
}

object Topic : IntIdTable() {
    val name = text("name")
    val fullName = text("full_name")
    val description = text("description")
    val image = text("image").nullable()
    val link = text("link").uniqueIndex()
    val stars = integer("stars")
    val watchers = integer("watchers")
    val updatedAt = timestamp("updatedAt")
    val pushedAt = timestamp("pushedAt")
    val createdAt = timestamp("createdAt")
    val topics = text("topics")
    val language = text("language")
}

class TopicDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TopicDao>(Topic) {
        const val SEPARATOR = "|||"
    }

    var name by Topic.name
    var fullName by Topic.fullName
    var description by Topic.description
    var image by Topic.image
    var link by Topic.link
    var stars by Topic.stars
    var watchers by Topic.watchers
    var language by Topic.language
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
        fullName = it.fullName,
        url = it.link,
        createdAt = it.createdAt.toString(),
        updatedAt = it.updatedAt.toString(),
        pushedAt = it.pushedAt.toString(),
        description = it.description,
        topics = it.topics,
        stars = it.stars,
        watchers = it.watchers,
        avatarUrl = it.image,
        language = it.language
    )
}

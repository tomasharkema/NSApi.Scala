package connection

import play.api.Logger
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by tomas on 14-06-15.
 */
object Connection {
  def getCollection(collectionName: String): BSONCollection = {
    val driver = new MongoDriver
    val connection = driver.connection(List(sys.env.getOrElse("PROD_MONGODB", "localhost")))

    // Gets a reference to the database "plugin"
    val db = connection(sys.env.getOrElse("PROD_MONGO_DB", "nsapi"))

    // Gets a reference to the collection "acoll"
    // By default, you get a BSONCollection.
    val collection = db(collectionName)
    collection
  }
}

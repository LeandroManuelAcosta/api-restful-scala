package app

import cask._
import models._
import scala.collection.script.Location
import scala.collection.mutable.ListBuffer
import upickle.default._

import upickle.default.{ReadWriter => RW, macroRW}

case class ItemJSON(name: String, amount: Int)
object ItemJSON {
  implicit val rw: RW[ItemJSON] = macroRW
}

object RestfulAPIServer extends MainRoutes  {
  override def host: String = "0.0.0.0"
  override def port: Int = 4000

  @get("/")
  def root(): Response = {
    println("\n\nNo filtrado: ")
    Location.all.foreach(println)
    
    println("Filtrado: ")
    Location.filter(Map("coordX" -> 10)).foreach(println)  
    JSONResponse("asdasd", 200)
  }

  /*
   *  Location routes
   *  - locations (GET)
   *  - locations (POST)
   */
  @get("/api/locations")
  def locations(): Response = {
    JSONResponse(Location.all.map(location => location.toMap))
  }

  @postJson("/api/locations")
  def locations(name: String, coordX: Int, coordY: Int): Response = {
    if (Location.exists("name", name)) {
      return JSONResponse("existing location name", 409)
    }

    val location = Location(name, coordX, coordY)
    location.save()
    JSONResponse(location.id)
  }

  // /*
  //  *  User routes
  //  *  - deleteUser (POST)
  //  */
  @post("/api/users/delete/:username")
  def deleteUser(username: String): Response = {
    
    if (Consumer.exists("username", username)) {
      val instanceUser = Consumer.all.find(
        consumer => consumer.username == username
      ).get
      Consumer.delete(instanceUser.id)
      return JSONResponse("Ok", 404)
    
    } else if (Provider.exists("username", username)) {
      val instanceUser = Provider.all.find(consumer => consumer.username == username).get
      Provider.delete(instanceUser.id)
      return JSONResponse("Ok", 404)
    }

    return JSONResponse("non existing user", 404)
    
  }

  /*
   *  Consumers routes
   *  - providers  (GET)
   *  - providers  (POST)
   */
  
  @get("/api/consumers")
  def consumers(): Response = {
    JSONResponse(Consumer.all.map(consumer => consumer.toMap))
  }

  @postJson("/api/consumers")
  def consumers(username: String, location: String): Response = {
    if (Consumer.exists("username", username)) {
      return JSONResponse("existing username", 409)
    }
    if (!Location.exists("name", location)) {
      return JSONResponse("not existing location", 409)
    }

    val locationInstance = Location.findByAttribute("name", location)
    
    val locationId = locationInstance.id

    val consumer = Consumer(username, locationId, 0)
    consumer.save()
    JSONResponse(consumer.id)
  }
  
  
  /*
   *  Providers routes
   *  - providers  (GET)
   *  - providers  (POST)
   */

  @get("/api/providers")
  def providers(locationName: String = ""): Response = {
    if (locationName == "") {
      return JSONResponse(Provider.all.map(provider => provider.toMap))
    }
    if (!Location.exists("name", locationName)) {
      return JSONResponse(Provider.all.map(provider => provider.toMap))      
    }
    val locationInstance = Location.findByAttribute("name", locationName)
    val locationId = locationInstance.id
    JSONResponse(Provider.filter(Map("locationId" -> locationId)).map(provider => provider.toMap))
  }

  @postJson("/api/providers")
  def providers(username: String, storeName: String, 
                location: String, maxDeliveryDistance: Int): Response = {
    if (Provider.exists("username", username) || Provider.exists("storeName", storeName) ) {
      return JSONResponse("existing username/storeName", 409)
    } else if (maxDeliveryDistance < 0) {
      return JSONResponse("negative maxDeliveryDistance ", 400)
    } else if (!Location.exists("name", location)) {
      return JSONResponse("non existing location", 409)
    }
    
    val locationInstance = Location.findByAttribute("name", location)    
    val locationId = locationInstance.id
    val provider = Provider(username, storeName, locationId, 0, maxDeliveryDistance)
    provider.save()
    JSONResponse(provider.id)
  }
 
  /*
   *  Orders routes
   *  - orders        (GET)
   *  - orders        (POST)
   *  - orders/detail (GET)
   */ 
  
  @get("api/orders/detail/:id")
  def ordersDetail(id: Int): Response = {
    val ordersInstance = Order.find(id) match {
      case Some(s) => s
      case _ => return JSONResponse("non existing order",404)
    }
    JSONResponse(ordersInstance.detail)
  }

  @get("/api/orders")
  def orders(username: String): Response = {
    if (!Consumer.exists("username", username)) {
      return JSONResponse("non existing user", 404)
    }
    JSONResponse(Order.filter(Map("consumerUsername" -> username)).map(order => order.noItem))
  }

  private def validItems(itemsJSON: Seq[Map[String,Any]], itemsProvider: List[Items]): Boolean = {
    itemsJSON.forall(
      item => itemsProvider.exists(
        itProvider => itProvider.toMap.get("name") == item.get("name")
      )
    )
  }

  private def parseJson(json: String) = {
    val parse = read[List[ItemJSON]](json)
    parse.map(x => Map("name" -> x.name, "amount" -> x.amount))
  }

  @postJson("/api/orders")
  def orders(providerUsername: String, consumerUsername: String, jsonItems: String): Response = {
    if (!Provider.exists("username", providerUsername) && !Consumer.exists("username", consumerUsername)) {
      return JSONResponse("non existing consumer/provider/item for provider", 404)
    }
    val provider = Provider.filter(Map("username" -> providerUsername)).head
    val consumer = Consumer.filter(Map("username" -> consumerUsername)).head
    val location = Location.find(consumer.locationId).get
    val itemsJSON = parseJson(jsonItems)

    val itemsProvider = Items.filter(Map("providerId" -> provider.id))

    if (!validItems(itemsJSON, itemsProvider)) {
      return JSONResponse("non existing consumer/provider/item for provider", 404)
    }

    var orderTotal: Float = 0

    itemsJSON.foreach(
      item => orderTotal += 
        itemsProvider.find(
          itemProvider => itemProvider.toMap.get("name") == item.get("name")
        ).get.getPrice() * item.get("amount").get.asInstanceOf[Int]
    )
   
    val items = itemsJSON.map(
      item => Map("id" -> provider.getItem(item.get("name").get.asInstanceOf[String]),
                  "amount" -> item.get("amount").get.asInstanceOf[Int]
      ) 
    )

    val order = Order(consumer.id, consumer.username, location.name, provider.id, provider.storeName,
                      orderTotal, "payed", items) 

    order.save()
    JSONResponse(order.id)
  }

  @post("/api/orders/delete/:id")
  def orderDelete(id : Int): Response = {
    val ordersInstance =  Order.find(id) match {
      case Some(s) => s
      case _ => return JSONResponse("non existing Order", 404)
    }
    Order.delete(ordersInstance.id)
    JSONResponse("OK",200)
  }
  
  @post("/api/orders/deliver/:id")
  def orderDeliver(id : Int): Response = {
    var order =  Order.find(id) match {
      case Some(s) => s
      case _ => return JSONResponse("non existing Order", 404)
    }
    order.status = "delivered"
    order.save()

    JSONResponse("Ok",200)  
  }
  
  /*
   *  Items routes
   *  - items  (GET)
   *  - items  (POST)
   *  - delete (POST)
   */
  
  @get("/api/items")
  def items(providerUsername: String = ""): Response = {
    if (providerUsername == "") {
      return JSONResponse(Items.all.map(item => item.toMap))
    }
    if (!Provider.exists("username", providerUsername)){
      return JSONResponse("non existing provider", 404)
    }
    
    val provider = Provider.findByAttribute("username", providerUsername)
    val itemsList = Items.filter(Map("providerId" -> provider.id))

    JSONResponse(itemsList.map(item => item.toMap))
  }

  @postJson("/api/items")
  def items(name: String, description: String, 
            price: Float, providerUsername: String): Response = {
    if (!Provider.exists("username", providerUsername)) {
      return JSONResponse("non existing provider", 404)
    }
    if (Items.exists("name", name)) {
      return JSONResponse("existing item for provider", 409)
    }

    val providerId = Provider.findByAttribute("username", providerUsername).id
    val item = Items(name, price, description, providerId)
    item.save()
    JSONResponse(providerId, 200)
  }

  @postJson("/api/items/delete")
  def delete(id: Int): Response = {
    if (Items.exists("id", id)) {
      Items.delete(id)
      JSONResponse("Ok", 200)
    }
    else {
      JSONResponse("non existing item", 404)
    }
  }


  override def main(args: Array[String]): Unit = {
    System.err.println("\n " + "=" * 39)
    System.err.println(s"| Server running at http://$host:$port ")

    if (args.length > 0) {
      val databaseDir = args(0)
      Database.loadDatabase(databaseDir)
      System.err.println(s"| Using database directory $databaseDir ")
    } else {
      Database.loadDatabase()  // Use default location
    }
    System.err.println(" " + "=" * 39 + "\n")

    super.main(args)
  }

  initialize()
}

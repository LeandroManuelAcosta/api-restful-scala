package models

object Order extends ModelCompanion[Order] {
  protected def dbTable: DatabaseTable[Order] = Database.orders

    def apply(consumerId: Int, consumerUsername: String,providerId: Int, 
              providerStoreName: String, orderTotal: Float,status: String, items: List[Map[String, Any]]): 
              Order = new Order(consumerId,consumerUsername,providerId,
                                  providerStoreName,orderTotal,status, items)

  private[models] def apply(jsonValue: JValue): Order = {
    val value = jsonValue.extract[Order]
    value._id = (jsonValue \ "id").extract[Int]
    value.save()
    value
  }
 }

class Order(val consumerId: Int,val consumerUsername: String,
             val providerId: Int,val providerStoreName: String, 
             val orderTotal: Float, val status: String, val items: List[Map[String, Any]]) extends Model[Order] {
  
  protected def dbTable: DatabaseTable[Order] = Order.dbTable

  override def toMap: Map[String, Any] = 
    super.toMap + ("consumerId"-> consumerId, "consumerUsername" -> consumerUsername,
                   "providerId" -> providerId, "providerStoreName" -> providerStoreName,
                   "orderTotal" -> orderTotal, "status" -> status, "items" -> items)

  override def toString: String = s"Order: $id"
}

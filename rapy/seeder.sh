http localhost:4000/api/locations/ name="nueva cordoba" coordX:=11 coordY:=2000020
http localhost:4000/api/locations/ name="alberdi" coordX:=10 coordY:=2000020
http localhost:4000/api/locations/ name="guemes" coordX:=9 coordY:=2000020

http localhost:4000/api/providers/ username="lapeti1" storeName="nombre local" location="guemes" maxDeliveryDistance:=1000
http localhost:4000/api/providers/ username="lapeti2" storeName="local de guemes" location="guemes" maxDeliveryDistance:=1000
http localhost:4000/api/providers/ username="lapeti3" storeName="nombre xdd" location="nueva cordoba" maxDeliveryDistance:=1000

http localhost:4000/api/consumers username="consumidor" location="guemes" 
http localhost:4000/api/consumers username="consumidor1" location="guemes" 
http localhost:4000/api/consumers username="consumidor2" location="guemes" 
http localhost:4000/api/consumers username="consumidor3" location="nueva cordoba" 

http localhost:4000/api/providers?locationName="guemes"
http localhost:4000/api/consumers

# http localhost:4000/api/orders providerUsername="lapeti1" consumerUsername="consumidor" items="asdasd"

http localhost:4000/api/orders providerUsername="lapeti1" consumerUsername="consumidor" jsonItems='[{"name":"Item 1","amount":2},{"name":"Item 2","amount":1}]'

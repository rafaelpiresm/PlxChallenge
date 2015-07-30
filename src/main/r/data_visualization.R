library(ggplot2)

model1 = data.frame(
  error = c(6163.637529718505,
            2177.613649102676,
            1359.1203995219853,
            931.3990318491135,
            692.9735405482966,
            592.2246116248544,
            510.4908835695487,
            411.11738875711865,
            348.84573687682433),
  clusterSize = c(2,4,6,8,10,12,14,16,18)
)

model2 = data.frame(
  error = c(1615.9117676550934,
            1022.0006350155459,
            755.0337291442221,
            602.7141539031596,
            514.5786111422542,
            438.9722992093025,
            389.5726377285465,
            350.56144472808376,
            316.5465970469296),
  clusterSize = c(2,4,6,8,10,12,14,16,18)
)

ggplot(data=model1,
  aes(x=clusterSize, y=error, colour=-error)) +
  geom_line(size=1.0) + 
  geom_point(size=3, fill="white")  
  
ggplot(data=model2,
  aes(x=clusterSize, y=error, colour=-error)) +
  geom_line(size=1.0) + 
  geom_point(size=3, fill="white")
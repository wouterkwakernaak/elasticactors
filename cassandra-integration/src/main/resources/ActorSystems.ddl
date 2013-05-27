create keyspace ElasticActors with strategy_options = {datacenter1:3};

use ElasticActors;

create column family ActorSystems
  with column_type = 'Standard'
  and key_validation_class = 'UTF8Type'
  and comparator = 'UTF8Type'
  and default_validation_class = 'UTF8Type'
  and column_metadata=[
    {column_name:nrOfShards,validation_class:IntegerType}
    {column_name:configurationClass,validation_class:UTF8Type}];

create column family MessageQueues
  with column_type = 'Standard'
  and key_validation_class = 'UTF8Type'
  and comparator = 'TimeUUIDType'
  and default_validation_class = 'BytesType';

create column family PersistentActors
  with column_type = 'Standard'
  and key_validation_class = 'UTF8Type'
  and comparator = 'UTF8Type'
  and default_validation_class = 'BytesType';

set ActorSystems['PiTest']['nrOfShards'] = int(8);
set ActorSystems['PiTest']['configurationClass'] = 'org.elasticsoftware.elasticactors.examples.pi.PiApproximator';
set ActorSystems['Http']['nrOfShards'] = int(8);
set ActorSystems['Http']['configurationClass'] = 'org.elasticsoftware.elasticactors.http.HttpActorSystem';
set ActorSystems['GeoEvents']['nrOfShards'] = int(8);
set ActorSystems['GeoEvents']['configurationClass'] = 'org.elasticsoftware.elasticactors.geoevents.GeoEventsActorSystem';
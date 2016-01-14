package demo

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._

import org.apache.avro.Schema
import org.apache.spark._

import spray.json.DefaultJsonProtocol._

class FileReaderSet(path: String)(implicit sc: SparkContext) extends ReaderSet {
  val attributeStore = FileAttributeStore(path)

  val metadataReader =
    new MetadataReader {
      def initialRead(layer: LayerId) = {
        val rmd = attributeStore.readLayerAttributes[FileLayerHeader, RasterMetaData, KeyBounds[SpaceTimeKey], KeyIndex[SpaceTimeKey], Schema](layer)._2
        val times = attributeStore.read[Array[Long]](LayerId(layer.name, 0), "times")
        LayerMetadata(rmd, times)
      }

      def layerNamesToZooms =
        attributeStore.layerIds
          .groupBy(_.name)
          .map { case (name, layerIds) => (name, layerIds.map(_.zoom).sorted.toArray) }
          .toMap
    }

  val singleBandLayerReader = FileLayerReader[SpaceTimeKey, Tile, RasterMetaData](attributeStore)
  val singleBandTileReader = new CachingTileReader(FileTileReader[SpaceTimeKey, Tile](path))

  val multiBandLayerReader = FileLayerReader[SpaceTimeKey, MultiBandTile, RasterMetaData](attributeStore)
  val multiBandTileReader = new CachingTileReader(FileTileReader[SpaceTimeKey, MultiBandTile](path))
}

/*
 * Copyright (C) 2015 Holmes Team at HUAWEI Noah's Ark Lab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.spark.streamdm.core

/**
 * A SparseInstance is an Instance in which the features are sparse, i.e., most
 * features will not have any value.
 * The SparseInstance will keep two Arrays: one with the values and one with the
 * corresponding indexes.
 */

case class SparseInstance(inIndexes:Array[Int], inValues:Array[Double])
  extends Instance with Serializable {
  
  type T = SparseInstance

  val indexes = inIndexes
  val values = inValues

  /* Get the value present at position index
  *
  * @param index the index of the features
  * @return a Double representing the value, or 0.0 if not found
  */
  def apply(index: Int): Double = {
    var i: Int = 0
    var value: Double = 0.0
    var found = false
    while(i<indexes.length && !found) {
      if(indexes(i)==index) {
        value = values(i)
        found = true
      }
      i += 1
    }
    value
  }

  /* Perform a dot product between two instances
  *
  * @param input an Instance with which the dot
  * product is performed
  * @return a Double representing the dot product 
  */
  override def dot(input: Instance): Double = {
    var i: Int = 0
    var dot: Double = 0.0
    while(i<indexes.length) {
      dot += values(i)*input(indexes(i))
      i += 1
    }
    dot
  }

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return a SparseInstance representing the added Instances
   */
  override def add(input: Instance): SparseInstance = input match {
    case SparseInstance(ind,v) => {
      var i: Int = 0
      var addedFeatures: Array[Double] = Array()
      var addedIndexes: Array[Int] = Array()
      while(i<ind.length) {
        val sum = v(i) + apply(ind(i))
        if(v(i)!=0 && sum!=0) {
          addedIndexes :+= ind(i)
          addedFeatures :+= sum
        }
        i += 1
      }
      i = 0
      while(i<indexes.length) {
        val other = input(indexes(i))
        if(other==0 && values(i)!=0) {
          addedIndexes :+= indexes(i)
          addedFeatures :+= values(i)
        }
        i += 1
      }
      new SparseInstance(addedIndexes, addedFeatures)
    }
    case DenseInstance(f) => {
      var i: Int = 0
      var addedFeatures: Array[Double] = Array()
      var addedIndexes: Array[Int] = Array()
      while(i<f.length) {
        val sum = f(i) + this(i)
        if(sum!=0) {
          addedIndexes :+= i
          addedFeatures :+= sum
        }
        i += 1
      }
      new SparseInstance(addedIndexes, addedFeatures)
    }
    case _ => this
  }

  /** Append a feature to the instance
   *
   * @param input the value which is added up
   * @return a SparseInstance representing the new feature vector
   */
  override def set(index: Int, input: Double): SparseInstance =
    new SparseInstance(indexes:+index,values:+input)

  /** Apply an operation to every feature of the Instance (essentially a map)
   * @param func the function for the transformation
   * @return a new SparseInstance with the transformed features
   */
  override def map(func: Double=>Double): SparseInstance =
    new SparseInstance(indexes, values.map{case x => func(x)})

    override def toString = (indexes zip values).map{ case (i,v) =>
                              "%d:%f".format(i+1,v)}.mkString(",")
  
}

object SparseInstance extends Serializable {
  
  /** Parse the input string as an SparseInstance class
   *
   * @param input the String line to be read, in LibSVM format
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): SparseInstance = {
    val tokens = input.split(",")
    val features = tokens.map(_.split(":"))
    new SparseInstance(features.map(_(0).toInt-1),features.map(_(1).toDouble))
  }
}

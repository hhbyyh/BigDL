/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.nn.abstractnn.TensorModule
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric

import scala.reflect.ClassTag

/**
 * Cropping layer for 3D data (e.g. spatial or spatio-temporal).
 *
 * # Input shape
 *     5D tensor with shape:
 *      (batchSize, channels, first_axis_to_crop, second_axis_to_crop, third_axis_to_crop)
 * # Output shape
 *      5D tensor with shape:
 *      (batchSize, channels, first_cropped_axis, second_cropped_axis, third_cropped_axis)
 *
 * @param dim1Crop How many units should be trimmed off at the beginning and end of
                     first cropping dimensions.
 * @param dim2Crop How many units should be trimmed off at the beginning and end of
 *                  the second dimension
 * @param dim3Crop How many units should be trimmed off at the beginning and end of
 *                  the third dimension
 * @param dataFormat: Cropping3D.CHANNEL_FIRST or Cropping3D.CHANNEL_LAST
 */
class Cropping3D[T: ClassTag](
    dim1Crop: Array[Int],
    dim2Crop: Array[Int],
    dim3Crop: Array[Int],
    dataFormat: String = Cropping3D.CHANNEL_FIRST
  )(implicit ev: TensorNumeric[T]) extends TensorModule[T] {

  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    require(input.dim() == 5, "input dimensions should be 5." +
      " (batchSize, channels, first_axis_to_crop, second_axis_to_crop, third_axis_to_crop)")

    val (dim1, dim2, dim3, dim1Start, dim1Cropped, dim2Start, dim2Cropped, dim3Start, dim3Cropped) =
      calculateStartAndLength(input)

    require(dim1Cropped > 0, s"dim1Crop: ${dim1Crop.mkString(", ")} is too large. dim1" +
      s" dimension length: ${input.size(dim1)}")
    require(dim2Cropped > 0, s"dim2Crop: ${dim2Crop.mkString(", ")} is too large. dim2" +
      s" dimension length: ${input.size(dim2)}")
    require(dim3Cropped > 0, s"dim3Crop: ${dim3Crop.mkString(", ")} is too large. dim3" +
      s" dimension length: ${input.size(dim3)}")

    this.output = input
      .narrow(dim1, dim1Start, dim1Cropped)
      .narrow(dim2, dim2Start, dim2Cropped)
      .narrow(dim3, dim3Start, dim3Cropped)
      .contiguous()
    output
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    val (dim1, dim2, dim3, dim1Start, dim1Cropped, dim2Start, dim2Cropped, dim3Start, dim3Cropped) =
      calculateStartAndLength(input)

    gradInput.resizeAs(input).zero()
      .narrow(dim1, dim1Start, dim1Cropped)
      .narrow(dim2, dim2Start, dim2Cropped)
      .narrow(dim3, dim3Start, dim3Cropped)
      .copy(gradOutput)
  }

  /**
   * Calculate the start position and length after cropping
   */
  private def calculateStartAndLength(input: Tensor[T]):
      (Int, Int, Int, Int, Int, Int, Int, Int, Int) = {
    val (dim1, dim2, dim3) = dataFormat match {
      case Cropping3D.CHANNEL_FIRST => (3, 4, 5)
      case Cropping3D.CHANNEL_LAST => (2, 3, 4)
      case _ => throw new IllegalArgumentException(s"$dataFormat is not a supported format")
    }

    val dim1Start = dim1Crop(0) + 1
    val dim1Cropped = input.size(dim1) - dim1Crop(0) - dim1Crop(1)
    val dim2Start = dim2Crop(0) + 1
    val dim2Cropped = input.size(dim2) - dim2Crop(0) - dim2Crop(1)
    val dim3Start = dim3Crop(0) + 1
    val dim3Cropped = input.size(dim3) - dim3Crop(0) - dim3Crop(1)

    (dim1, dim2, dim3, dim1Start, dim1Cropped, dim2Start, dim2Cropped, dim3Start, dim3Cropped)
  }

  override def clearState(): this.type = {
    super.clearState()
    this
  }

  override def toString(): String = {
    s"$getPrintName(dim1: ${dim1Crop.mkString(", ")};" +
      s" dim2Crop: ${dim2Crop.mkString(", ")};" +
      s" dim3Crop: ${dim3Crop.mkString(", ")})"
  }
}

object Cropping3D {

  val CHANNEL_FIRST = "channel_first"
  val CHANNEL_LAST = "channel_last"

  def apply[T: ClassTag](
      dim1Crop: Array[Int],
      dim2Crop: Array[Int],
      dim3Crop: Array[Int],
      format: String = Cropping3D.CHANNEL_FIRST)(implicit ev: TensorNumeric[T]): Cropping3D[T] = {
    new Cropping3D[T](dim1Crop, dim2Crop, dim3Crop, format)
  }
}

/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.bankaccountinsightsstub.controllers

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.bankaccountinsightsstub.model.BankAccountDetails.implicits.*
import uk.gov.hmrc.bankaccountinsightsstub.model.{BankAccountDetails, BankAccountInsightsResponse}

class BankAccountRiskListsControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val testCorrelationId = "test-correlation-id-123"

  private val controller = new BankAccountRiskListsController(Helpers.stubControllerComponents())

  private val injector           = app.injector
  implicit val mat: Materializer = injector.instanceOf[Materializer]

  "POST /reject/nino" should {
    "return 400 for malformed request payload" in {
      val malformedRequest = FakeRequest("POST", "/reject/nino")
        .withBody(Json.parse("""{"invalidField": "value"}"""))
        .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)

      val result           = controller.isBankAccountOnRejectList()(malformedRequest)
      status(result)        shouldBe Status.BAD_REQUEST
      contentAsJson(result) shouldBe Json.obj(
        "/accountNumber" -> Json.arr("error.path.missing"),
        "/sortCode"      -> Json.arr("error.path.missing")
      )
    }

    "return 200 with result true for bank account on reject list" in {
      val validRequest = FakeRequest("POST", "/reject/nino")
        .withBody(Json.toJson(BankAccountDetails("393358", "13902323")))
        .withHeaders(
          CONTENT_TYPE    -> MimeTypes.JSON,
          "CorrelationId" -> testCorrelationId
        )

      val result = controller.isBankAccountOnRejectList()(validRequest)

      status(result)        shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(BankAccountInsightsResponse.onWatchlistResponse(testCorrelationId))
    }

    "return 200 with result false for bank account not on reject list" in {
      val validRequest = FakeRequest("POST", "/reject/nino")
        .withBody(Json.toJson(BankAccountDetails("111111", "22222222")))
        .withHeaders(
          CONTENT_TYPE    -> MimeTypes.JSON,
          "CorrelationId" -> testCorrelationId
        )

      val result = controller.isBankAccountOnRejectList()(validRequest)

      status(result)        shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(BankAccountInsightsResponse.notOnWatchlistResponse(testCorrelationId))
    }
  }
}

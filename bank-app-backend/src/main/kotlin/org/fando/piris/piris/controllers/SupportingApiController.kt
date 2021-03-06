package org.fando.piris.piris.controllers

import org.fando.piris.piris.models.*
import org.fando.piris.piris.services.AccountService
import org.fando.piris.piris.services.SupportingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("supporting")
@CrossOrigin(origins = ["*"], allowedHeaders = ["*"])
class SupportingApiController @Autowired constructor(
    private val accountService: AccountService,
    private val supportingService: SupportingService
) {

    @GetMapping("countries")
    fun getCountries() = ResponseEntity.ok(CountriesEnum.values())

    @GetMapping("disabilities")
    fun getDisabilities() = ResponseEntity.ok(DisabilityEnum.values().map { it })

    @GetMapping("martial-statuses")
    fun getMartialStatuses() = ResponseEntity.ok(FamilyStatusEnum.values().map { it })

    @GetMapping("gender")
    fun getGenders() = ResponseEntity.ok(GenderEnum.values().map { it })

    @GetMapping("currency")
    fun getCurrencies() = ResponseEntity.ok(CurrencyEnum.values().map { it })

    @GetMapping("cities")
    fun getCities(@RequestParam("country") country: CountriesEnum) = when (country) {
        CountriesEnum.BLR -> ResponseEntity.ok(BelarusCities.values().map { it.city })
        CountriesEnum.RUS -> ResponseEntity.ok(RussiaCities.values().map { it.city })
        CountriesEnum.UKR -> ResponseEntity.ok(UkraineCities.values().map { it.city })
        else -> ResponseEntity.badRequest().body("Invalid country passed")
    }

    @GetMapping("closeDate")
    fun getCurrentDate(): ResponseEntity<Any> {
        return ResponseEntity.ok(ResponseCloseDate(supportingService.getCurrentDate().currDate))
    }

    @PostMapping("close")
    fun closeDay(@RequestBody body: RequestCloseDay): ResponseEntity<Any> {
        val currentDate = supportingService.getCurrentDate().currDate
        accountService.closeDay(body.closeUntil, currentDate)
        return ResponseEntity.ok().build()
    }
}
package org.fando.piris.piris.services

import jdk.nashorn.internal.parser.DateParser.DAY
import org.fando.piris.piris.entities.Account
import org.fando.piris.piris.entities.Client
import org.fando.piris.piris.entities.Contract
import org.fando.piris.piris.models.AccountCodes
import org.fando.piris.piris.models.AccountTypeEnum
import org.fando.piris.piris.models.StatusEnum
import org.fando.piris.piris.repositories.AccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.transaction.Transactional
import kotlin.random.Random

@Service
@Transactional
class AccountService @Autowired constructor(
        private val accountRepository: AccountRepository
) {

    fun closeAccounts(contract: Contract) {
        val accounts = accountRepository.findAccountsByContract(contract);
        val bankAccount = accountRepository.findAccountByAccountCode(AccountCodes.BDF.code)
        accounts.map { acc ->
            acc.status = StatusEnum.CLOSED
            if (acc.accountCode == AccountCodes.DEP.code) {
                acc.credit.plus(contract.amount)
                bankAccount.debit.plus(contract.amount)
            }
            accountRepository.save(acc)
        }
        accountRepository.save(bankAccount)
    }

    fun getAllAccounts(): List<Account> = accountRepository.findAll()

    fun replenishBankAccount(amount: BigDecimal) {
        val bankAccount = accountRepository.findAccountByAccountCode(AccountCodes.BDF.code)
        bankAccount.credit.plus(amount)
    }

    fun getFromBankAccount(amount: BigDecimal) {
        val bankAccount = accountRepository.findAccountByAccountCode(AccountCodes.BDF.code)
        bankAccount.debit.plus(amount)
    }

    fun createDepositAccounts(client: Client, contract: Contract) {
        createDepositAccount(client, contract)
        createPercentsDepositAccount(client, contract)
    }

    fun createCreditAccounts(client: Client, contract: Contract) {
        createCreditAccount(client, contract)
        createPercentsCreditAccount(client, contract)
    }

    fun closeDay() {
        val accounts = accountRepository.findAll()
        val bankAccount = accounts.first { it.accountCode == AccountCodes.BDF.code }
        val percentsDepAccounts = accounts.filter { it.accountCode == AccountCodes.DEPPRC.code }
        val debitAccounts = accounts.filter { it.accountCode == AccountCodes.DEP.code }
        percentsDepAccounts.forEach {
            val contract = it.contract
            val depositTermInDays = ChronoUnit.DAYS.between(contract.contractStartDate, contract.contractEndDate)
            val percentsAmount = contract.amount.multiply(contract.percents).multiply(BigDecimal(depositTermInDays)).divide(BigDecimal(365))
            val totalAmount = percentsAmount.divide(BigDecimal(100))
            it.credit.plus(totalAmount)
            bankAccount.debit.plus(totalAmount)
            it.surplus = it.credit.minus(it.debit)
            if (LocalDate.now().compareTo(contract.contractEndDate) == 0) {
                contract.status = StatusEnum.CLOSED
                it.status = StatusEnum.CLOSED
            }
            accountRepository.save(it)
        }
        debitAccounts.forEach {
            val contract = it.contract
            if (LocalDate.now().compareTo(contract.contractEndDate) == 0) {
                contract.status = StatusEnum.CLOSED
                it.status = StatusEnum.CLOSED
                it.credit = contract.amount
                it.surplus = it.credit.minus(it.debit)
                bankAccount.debit = contract.amount
            }
            accountRepository.save(it)
        }
        bankAccount.surplus = bankAccount.credit.minus(bankAccount.debit)
        accountRepository.save(bankAccount)
    }

    private fun createCreditAccount(client: Client, contract: Contract) {
        accountRepository.save(
                Account(
                        AccountCodes.CRD.code,
                        generateAccountNumber(AccountCodes.CRD.code, client),
                        AccountTypeEnum.ACTIVE,
                        BigDecimal(0),
                        BigDecimal(0),
                        BigDecimal(0),
                        client,
                        StatusEnum.ACTIVE,
                        contract
                )
        )
    }

    private fun createPercentsCreditAccount(client: Client, contract: Contract) {
        accountRepository.save(
                Account(
                        AccountCodes.CRDPRC.code,
                        generateAccountNumber(AccountCodes.CRDPRC.code, client),
                        AccountTypeEnum.PASSIVE,
                        BigDecimal(0),
                        BigDecimal(0),
                        BigDecimal(0),
                        client,
                        StatusEnum.ACTIVE,
                        contract
                )
        )
    }

    private fun createDepositAccount(client: Client, contract: Contract) {
        accountRepository.save(
                Account(
                        AccountCodes.DEP.code,
                        generateAccountNumber(AccountCodes.DEP.code, client),
                        AccountTypeEnum.ACTIVE,
                        BigDecimal(0),
                        BigDecimal(0),
                        BigDecimal(0),
                        client,
                        StatusEnum.ACTIVE,
                        contract
                )
        )
    }

    private fun createPercentsDepositAccount(client: Client, contract: Contract) {
        accountRepository.save(
                Account(
                        AccountCodes.DEPPRC.code,
                        generateAccountNumber(AccountCodes.DEPPRC.code, client),
                        AccountTypeEnum.PASSIVE,
                        BigDecimal(0),
                        BigDecimal(0),
                        BigDecimal(0),
                        client,
                        StatusEnum.ACTIVE,
                        contract
                )
        )
    }

    private fun generateAccountNumber(accountCode: Int, client: Client): String = """
        |$accountCode
        |${client.idDocument.passportNumber.substring(0, 5)}
        |${(client.accounts.size + 1).toString().padStart(3, '0')}
        |${Random.nextInt(0, 9)}
        |""".trimMargin()

}
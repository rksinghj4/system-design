package com.raj.systemdesignsamples.splitwise

import android.R.attr.name
import java.text.DecimalFormat
import kotlin.math.abs


enum class SplitType {
    EQUAL,
    EXACT,
    PERCENT
}

class Split(val userId: String, val amount: Double)

data class Expense(
    val id: String,
    val desc: String,
    val totalAmount: Double,
    val paidByUserId: String,
    val splits: List<Split>,
    val splitType: SplitType,
    //val createdAt: LocalDateTime,
    //val updatedAt: LocalDateTime,
    val groupId: String?
) {

    companion object {
        private var nextExpenseId = 0
        fun createExpense(
            desc: String,
            totalAmount: Double,
            paidByUserId: String,
            splits: List<Split>,
            splitType: SplitType,
            groupId: String?
        ): Expense {
            val expenseId = "expense_" + nextExpenseId++
            return Expense(
                id = expenseId,
                desc = desc,
                totalAmount = totalAmount,
                paidByUserId = paidByUserId,
                splits = splits,
                splitType = splitType,
                groupId = groupId
            )
        }
    }
}


interface SplitStrategy {
    fun calculateSplits(
        totalAmount: Double,
        involvedUserIds: List<String>,
        splitValues: List<Double>
    ): List<Split>
}

class EqualSplitStrategy : SplitStrategy {
    override fun calculateSplits(
        totalAmount: Double,
        involvedUserIds: List<String>,
        splitValues: List<Double>
    ): List<Split> {
        val splitAmount = totalAmount / involvedUserIds.size
        return involvedUserIds.map { Split(it, splitAmount) }
    }
}

class ExactSplitStrategy : SplitStrategy {
    override fun calculateSplits(
        totalAmount: Double,
        involvedUserIds: List<String>,
        splitValues: List<Double>
    ): List<Split> {
        if (involvedUserIds.size != splitValues.size) {
            throw RuntimeException("Number of users and split values do not match for Exact Split")
        }

        val totalSplitAmount = splitValues.sum()
        if (Math.abs(totalSplitAmount - totalAmount) > 0.01) {
            throw RuntimeException("Total split amounts do not sum up to total amount for Exact Split")
        }
        //map returns a list of Split objects by zipping userIds and splitValues
        return involvedUserIds.zip(splitValues).map { (userId, amount) -> Split(userId, amount) }
    }
}

class PercentSplitStrategy : SplitStrategy {
    override fun calculateSplits(
        totalAmount: Double,
        involvedUserIds: List<String>,
        splitValues: List<Double>
    ): List<Split> {
        if (involvedUserIds.size != splitValues.size) {
            throw RuntimeException("Number of users and split values do not match for Percent Split")
        }

        val totalPercent = splitValues.sum()
        if (Math.abs(totalPercent - 100.0) > 0.01) {
            throw RuntimeException("Total percentage does not sum up to 100 for Percent Split")
        }

        return involvedUserIds.zip(splitValues).map { (userId, percent) ->
            val amount = (percent / 100) * totalAmount
            Split(userId, amount)
        }
    }
}

class SplitStrategyFactory {
    companion object {
        fun getSplitStrategy(splitType: SplitType): SplitStrategy {
            return when (splitType) {
                SplitType.EQUAL -> EqualSplitStrategy()
                SplitType.EXACT -> ExactSplitStrategy()
                SplitType.PERCENT -> PercentSplitStrategy()
            }
        }
    }
}

interface Observer {
    fun update(message: String)
}

class User(val id: String, val name: String, email: String) : Observer {
    private val expenses: MutableList<Expense> = mutableListOf()

    //// userId -> amount (positive = they owe you, negative = you owe them)
    val balancesWithOthers: MutableMap<String, Double> = mutableMapOf()

    fun addExpense(expense: Expense) {
        expenses.add(expense)
    }

    fun getExpenses(): List<Expense> {
        return expenses
    }

    override fun update(message: String) {
        println("User $name has been notified with : $message")
    }

    fun updateBalance(otherUserId: String, amount: Double) {
        balancesWithOthers[otherUserId] = balancesWithOthers.getOrDefault(otherUserId, 0.0) + amount

        if (Math.abs(balancesWithOthers[otherUserId]!!) < 0.01) {
            balancesWithOthers.remove(otherUserId)
        }
    }

    fun getBalances(): Map<String, Double> {
        return balancesWithOthers
    }

    //You Owe money - Tumne paisa liya
    //Lent/Give- tumne paisa diya


    fun getTotalBorrowing(): Double {//  total amount this user will give back to others
        return balancesWithOthers.values.filter { it < 0 }.sumOf { -it }
    }

    fun getTotalReceiving(): Double {// total amount this user will get from others
        return balancesWithOthers.values.filter { it > 0 }.sum()
    }

    companion object {
        private var nextUserId = 0
        fun createUser( name: String, email: String): User {
            return User(id = "user_" + nextUserId++, name, email)
        }
    }
}

class Group(val id: String, val groupName: String, val members: MutableList<User>) {
    //<expenseId, Expense>
    private val groupExpenses: MutableMap<String, Expense> = mutableMapOf()

    //<useId, <otherUserId, amount>>
    private var groupBalances: MutableMap<String, MutableMap<String, Double>> = mutableMapOf()

    fun addMember(user: User) {
        members.add(user)
        // Initialize balance map for new member
        groupBalances[user.id] = mutableMapOf()
        println("User ${user.name} added to group $groupName")
    }

    fun removeMember(user: User): Boolean {
        if (!canUserLeaveTheGroup(user)) {
            println("User ${user.name} cannot be removed from group $groupName due to unsettled balances")
            return false
        }
        members.remove(user)
        println("User ${user.name} removed from group $groupName")
        return true
    }

    private fun notifyAllObservers(message: String) {
        for (observer in members) {
            observer.update(message)
        }
    }

    fun isMember(userId: String): Boolean {
        return members.any { it.id == userId }
    }

    fun getUserByUserId(userId: String): User? {
        var user: User? = null

        for (member in members) {
            if (member.id == userId) {
                user = member
            }
        }
        return user
    }

    fun canUserLeaveTheGroup(user: User): Boolean {
        val balancesSheetWithOthers =
            groupBalances[user.id] ?: throw RuntimeException("User is not a part of this group")

        for (amount in balancesSheetWithOthers.values) {
            if (Math.abs(amount) > 0.01) {
                return false
            }
        }
        return true
    }

    fun getUserGroupBalances(user: User): Map<String, Double> {
        return groupBalances[user.id] ?: throw RuntimeException("User is not a part of this group")
    }

    fun updateGroupBalances(expense: Expense) {
        for (split in expense.splits) {
            if (split.userId == expense.paidByUserId) continue

            val payerBalanceSheetWithOthers = groupBalances[expense.paidByUserId]
                ?: throw RuntimeException("Payer not found in group balances")
            payerBalanceSheetWithOthers[split.userId] =
                payerBalanceSheetWithOthers.getOrDefault(split.userId, 0.0) + split.amount

            val payeeBalanceSheetWithOthers = groupBalances[split.userId]
                ?: throw RuntimeException("Payee not found in group balances")
            payeeBalanceSheetWithOthers[expense.paidByUserId] =
                payeeBalanceSheetWithOthers.getOrDefault(expense.paidByUserId, 0.0) - split.amount

            //Remove zero balances
            if (Math.abs(payerBalanceSheetWithOthers[split.userId]!!) < 0.01) {
                payerBalanceSheetWithOthers.remove(split.userId)
            }

            if (Math.abs(payeeBalanceSheetWithOthers[expense.paidByUserId]!!) < 0.01) {
                payeeBalanceSheetWithOthers.remove(expense.paidByUserId)
            }

        }
    }

    // Update balance within group
    fun updateGroupBalance(fromUserId: String, toUserId: String, amount: Double) {
        groupBalances[fromUserId]!![toUserId] =
            groupBalances[fromUserId]!!.getOrDefault(toUserId, 0.0) + amount
        groupBalances[toUserId]!![fromUserId] =
            groupBalances[toUserId]!!.getOrDefault(fromUserId, 0.0) - amount


        // Remove if balance becomes zero
        if (Math.abs(groupBalances[fromUserId]!![toUserId]!!) < 0.01) {
            groupBalances[fromUserId]!!.remove(toUserId)
        }
        if (Math.abs(groupBalances[toUserId]!![fromUserId]!!) < 0.01) {
            groupBalances[toUserId]!!.remove(fromUserId)
        }
    }

    fun settlePayment(fromUserId: String?, toUserId: String?, amount: Double): Boolean {
        // Validate that both users are group members
        if (!isMember(fromUserId!!) || !isMember(toUserId!!)) {
            println("user is not a part of this group")
            return false
        }


        // Update group balances
        updateGroupBalance(fromUserId, toUserId, amount)


        // Get user names for display
        val fromName = getUserByUserId(fromUserId!!)!!.name
        val toName = getUserByUserId(toUserId!!)!!.name


        // Notify group members
        notifyAllObservers("Settlement: $fromName paid $toName Rs $amount")

        println(
            ("Settlement in " + name + ": " + fromName + " settled Rs "
                    + amount + " with " + toName)
        )

        return true
    }

    fun addExpense(
        description: String?, amount: Double, paidByUserId: String,
        involvedUsers: List<String>, splitType: SplitType
    ): Boolean {
        return addExpense(
            description,
            amount,
            paidByUserId,
            involvedUsers,
            splitType,
            ArrayList<Double>()
        )
    }


    internal fun addExpense(
        desc: String?,
        totalAmount: Double,
        paidByUserId: String,
        involvedUserIds: List<String>,
        splitType: SplitType,
        splitValues: List<Double>
    ): Boolean {

        //Check if payer is member of group
        if (isMember(paidByUserId).not()) {
            throw RuntimeException("Payer is not a member of the group")
        }

        //Check if all involved users are members of group
        involvedUserIds.forEach {
            isMember(it).not().let { notMember ->
                if (notMember) {
                    throw RuntimeException("Involved user $it is not a member of the group")
                }
            }
        }


        val splitStrategy = SplitStrategyFactory.getSplitStrategy(splitType)
        val splits = splitStrategy.calculateSplits(totalAmount, involvedUserIds, splitValues)

        val expense = Expense.createExpense(
            desc = desc ?: "No Description",
            totalAmount = totalAmount,
            paidByUserId = paidByUserId,
            splits = splits,
            splitType = splitType,
            groupId = this.id
        )

        groupExpenses[expense.id] = expense

        updateGroupBalances(expense)

        println("\n=========== Sending Notifications ====================")
        val paidByName: String = getUserByUserId(paidByUserId)!!.name
        notifyAllObservers("New expense added in group $groupName: ${expense.desc}, Amount: ${expense.totalAmount}")


        // Printing console message-------
        println("\n=========== Expense Message ====================")
        println("Expense added to $groupName: $desc  (Rs $totalAmount ) paid by $paidByName and involved people are :")

        if (!splitValues.isEmpty()) {
            involvedUserIds.forEachIndexed { i, userId ->
                println(getUserByUserId(userId)?.name + " : " + splitValues[i])
            }
        } else {
            println("Will be paid equally Rs ${totalAmount / involvedUserIds.size} each among following members: ")
            involvedUserIds.forEach { userId ->
                println(getUserByUserId(userId)?.name + ", ")
            }
        }

        return true
    }

    fun showGroupBalances() {
        println("""=== Group Balances for $groupName ===""")
        val df: DecimalFormat = DecimalFormat("#.##")
        for ((memberId, userBalances) in groupBalances) {
            val memberName = getUserByUserId(memberId)?.name
            println("$memberName's balances in group:")

            if (userBalances.isEmpty()) {
                println("  No outstanding balances")
            } else {
                for ((otherMemberUserId, balance) in userBalances) {
                    val otherName = getUserByUserId(otherMemberUserId)!!.name
                    if (balance > 0) {
                        println("  " + otherName + " owes: Rs " + df.format(balance))
                    } else {
                        println("  Owes " + otherName + ": Rs " + df.format(abs(balance)))
                    }
                }
            }
        }
    }

    fun simplifyGroupDebts() {
        val simplifiedBalances: Map<String, Map<String, Double>> =
            DebtSimplifier.simplifyDebts(groupBalances)
        groupBalances = simplifiedBalances as MutableMap<String, MutableMap<String, Double>>

        println(
            """
            Debts have been simplified for group: $groupName
            """.trimIndent()
        )
    }

    companion object {
        private var nextGroupId = 0
        fun createGroup(groupName: String, members: MutableList<User>): Group {
            val groupId = "group_" + nextGroupId++
            return Group(id = groupId, groupName = groupName, members = members)
        }
    }

}
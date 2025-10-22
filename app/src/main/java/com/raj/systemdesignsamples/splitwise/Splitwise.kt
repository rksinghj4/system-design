package com.raj.systemdesignsamples.splitwise

import java.text.DecimalFormat
import kotlin.math.abs


// Main ExpenseManager class (Singleton - Facade)
class Splitwise private constructor() {
    private val users: MutableMap<String, User> =
        HashMap()
    private val groups: MutableMap<String, Group> =
        HashMap()
    private val expenses: MutableMap<String, Expense> = HashMap()

    // User management
    fun createUser(name: String, email: String?): User {
        val user = User(
            name,
            email!!
        )
        users[user.id] = user
        println(("User created: " + name + " (ID: " + user.id).toString() + ")")
        return user
    }

    fun getUser(userId: String): User? {
        return users[userId]
    }

    // Group management
    fun createGroup(name: String): Group {
        val group = Group.createGroup(name, mutableListOf<User>())
        groups[group.id] = group
        println("Group created: $name (ID: + group.id) )")
        return group
    }

    fun getGroup(groupId: String): Group? {
        return groups[groupId]
    }

    fun addUserToGroup(userId: String, groupId: String) {
        val user = getUser(userId)
        val group = getGroup(groupId)

        if (user != null && group != null) {
            group.addMember(user)
        }
    }

    // Try to remove user from group - just delegates to group
    fun removeUserFromGroup(userId: String, groupId: String): Boolean {
        val group = getGroup(groupId)

        if (group == null) {
            println("Group not found!")
            return false
        }

        val user = getUser(userId)
        if (user == null) {
            println("User not found!")
            return false
        }

        val userRemoved = group.removeMember(users.get(userId)!!)

        if (userRemoved) {
            println(user.name + " successfully left " + group.groupName)
        }
        return userRemoved
    }

    // Expense management - delegate to group
    @JvmOverloads
    fun addExpenseToGroup(
        groupId: String, description: String?, amount: Double,
        paidByUserId: String?, involvedUsers: List<String>,
        splitType: SplitType?, splitValues: List<Double> = ArrayList()
    ) {
        val group = getGroup(groupId)
        if (group == null) {
            println("Group not found!")
            return
        }

        group.addExpense(
            description, amount,
            paidByUserId!!, involvedUsers, splitType!!, splitValues
        )
    }

    // Settlement - delegate to group
    fun settlePaymentInGroup(
        groupId: String, fromUserId: String?,
        toUserId: String?, amount: Double
    ) {
        val group = getGroup(groupId)
        if (group == null) {
            println("Group not found!")
            return
        }

        group.settlePayment(fromUserId, toUserId, amount)
    }

    // Settlement
    fun settleIndividualPayment(fromUserId: String, toUserId: String, amount: Double) {
        val fromUser = getUser(fromUserId)
        val toUser = getUser(toUserId)

        if (fromUser != null && toUser != null) {
            fromUser.updateBalance(toUserId, amount)
            toUser.updateBalance(fromUserId, -amount)

            println(fromUser.name + " settled Rs" + amount + " with " + toUser.name)
        }
    }

    @JvmOverloads
    fun addIndividualExpense(
        description: String, amount: Double, paidByUserId: String,
        toUserId: String, splitType: SplitType?,
        splitValues: List<Double> = ArrayList()
    ) {
        val strategy = SplitStrategyFactory.getSplitStrategy(splitType!!)
        val splits: List<Split> =
            strategy.calculateSplits(amount, listOf(paidByUserId, toUserId), splitValues)

        val expense =
            Expense.createExpense(description, amount, paidByUserId, splits, splitType, null)
        expenses[expense.id] = expense

        val paidByUser = getUser(paidByUserId)
        val toUser = getUser(toUserId)

        paidByUser?.updateBalance(toUserId, amount)
        toUser?.updateBalance(paidByUserId, -amount)

        println(
            ("Individual expense added: " + description + " (Rs " + amount
                    + ") paid by " + paidByUser!!.name + " for " + toUser!!.name)
        )
    }

    // Display Method
    fun showUserBalance(userId: String) {
        val user = getUser(userId) ?: return

        val df: DecimalFormat = DecimalFormat("#.##")
        println(
            """=========== Balance for ${user.name} ===================="""
        )
        println("Total you owe: Rs " + df.format(user.getTotalBorrowing()))
        println("Total others owe you: Rs " + df.format(user.getTotalReceiving()))

        println("Detailed balances:")
        for ((otherUserId, amount) in user.balancesWithOthers.entries) {
            val otherUser = getUser(otherUserId)
            if (otherUser != null) {
                if (amount > 0) {
                    println("  " + otherUser.name + " owes you: Rs" + amount)
                } else {
                    println("  You owe " + otherUser.name + ": Rs" + abs(amount))
                }
            }
        }
    }

    fun showGroupBalances(groupId: String) {
        val group = getGroup(groupId) ?: return

        group.showGroupBalances()
    }

    fun simplifyGroupDebts(groupId: String) {
        val group = getGroup(groupId) ?: return


        // Use group's balance data for debt simplification
        group.simplifyGroupDebts()
    }

    companion object {
        //Singleton instance
        val instance: Splitwise by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Splitwise() }
    }
}

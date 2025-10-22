package com.raj.systemdesignsamples.splitwise

import kotlin.math.min


class DebtSimplifier {
    companion object {
        fun simplifyDebts(
            groupBalances: Map<String, Map<String, Double>>
        ): Map<String, MutableMap<String, Double>> {

            // Calculate net amount for each person
            val netAmounts: MutableMap<String, Double> = HashMap()

            // Initialize all users with 0
            for (userId in groupBalances.keys) {
                netAmounts[userId] = 0.0
            }

            // Calculate net amounts
            // We only need to process each balance once (not twice)
            // If groupBalances[A][B] = 200, it means B owes A 200
            // So A should receive 200 (positive) and B should pay 200 (negative)
            for ((creditorId, creditorBalanceSheet) in groupBalances) {
                for ((debtorId, amount) in creditorBalanceSheet) {
                    // Only process positive amounts to avoid double counting
                    if (amount > 0) {
                        netAmounts[creditorId] =
                            netAmounts[creditorId]!! + amount // creditor receives
                        netAmounts[debtorId] = netAmounts[debtorId]!! - amount // debtor pays
                    }
                }
            }

            // Divide users into creditors and debtors
            val creditors: MutableList<MutableMap.MutableEntry<String, Double>> =
                mutableListOf() // those who should receive money

            val debtors: MutableList<MutableMap.MutableEntry<String, Double>> =
                mutableListOf() // those who should pay money

            for ((userId, amount) in netAmounts) {
                if (amount > 0.01) { // creditor
                    val mapEntry: MutableMap.MutableEntry<String, Double> =
                        mutableMapOf(userId to amount).entries.first()
                    creditors.add(mapEntry)
                    //creditors.add(Map.Entry(key, value))
                } else if (amount < -0.01) { // debtor
                    // store positive amount
                    val mapEntry: MutableMap.MutableEntry<String, Double> =
                        mutableMapOf(userId to -amount).entries.first()
                    debtors.add(mapEntry)
                    //debtors.add(Map.Entry(key, -value)) // store positive amount
                }
            }

            //creditors.sortBy { it.value } // ascending
            creditors.sortByDescending { it.value }
            creditors.forEach { println("${it.key} owes ${it.value}") }

            debtors.sortByDescending { it.value } // descending
            debtors.forEach { println("${it.key} is owed ${it.value}") }

            // Create new simplified balance map
            val simplifiedBalances: MutableMap<String, MutableMap<String, Double>> = HashMap()

            // Initialize empty maps for all users
            for (userId in groupBalances.keys) {
                simplifiedBalances[userId] = HashMap()
            }

            // Use greedy algorithm to minimize transactions
            var i = 0
            var j = 0
            while (i < creditors.size && j < debtors.size) {
                val creditorId: String = creditors[i].key
                val debtorId: String = debtors[j].key
                val creditorAmount: Double = creditors[i].value
                val debtorAmount: Double = debtors[j].value


                // Find the minimum amount to settle
                val settleAmount = min(creditorAmount, debtorAmount)


                // Update simplified balances
                // debtorId owes creditorId the settleAmount
                simplifiedBalances[creditorId]!![debtorId] = settleAmount

                simplifiedBalances[debtorId]!![creditorId] = -settleAmount

                // Update remaining amounts
                creditors[i].setValue(creditorAmount - settleAmount)
                debtors[j].setValue(debtorAmount - settleAmount)

                // Move to next creditor or debtor if current one is settled
                if (creditors[i].value < 0.01) {
                    i++
                }
                if (debtors[j].value < 0.01) {
                    j++
                }
            }

            return simplifiedBalances
        }
    }



}



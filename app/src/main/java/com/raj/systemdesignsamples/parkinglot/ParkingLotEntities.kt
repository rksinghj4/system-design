package com.raj.systemdesignsamples.parkinglot

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime


enum class VehicleSize {
    SMALL, MEDIUM, LARGE
}

interface Vehicle {
    val vehicleSize: VehicleSize
    fun getLicensePlateNumber(): String
}

data class Motorcycle(
    private val licensePlateNumber: String
) : Vehicle {
    override val vehicleSize: VehicleSize
        get() = VehicleSize.SMALL

    override fun getLicensePlateNumber(): String {
        return licensePlateNumber
    }
}

data class Car(
    private val licensePlateNumber: String
) : Vehicle {
    override val vehicleSize: VehicleSize
        get() = VehicleSize.MEDIUM

    override fun getLicensePlateNumber(): String {
        return licensePlateNumber
    }
}

data class Bus(
    private val licensePlateNumber: String
) : Vehicle {

    override val vehicleSize: VehicleSize
        get() = VehicleSize.LARGE

    override fun getLicensePlateNumber(): String {
        return licensePlateNumber
    }
}

enum class ParkingSpotType(val size: VehicleSize) {
    COMPACT(VehicleSize.SMALL),
    REGULAR(VehicleSize.MEDIUM),
    LARGE(VehicleSize.LARGE)
}

interface ParkingSpot {
    val spotNumber: String
    val spotType: ParkingSpotType
    var vehicle: Vehicle?
    var isAvailable: Boolean
    fun occupy(vehicle: Vehicle?) {
        synchronized(LazyThreadSafetyMode.SYNCHRONIZED) {
            if (isAvailable) {
                this.vehicle = vehicle
                isAvailable = false
            } else {
                throw IllegalStateException("Parking Spot $spotNumber is already occupied")
            }
        }
    }

    fun vacate() {
        synchronized(LazyThreadSafetyMode.SYNCHRONIZED) {
            if (isAvailable) {
                throw IllegalStateException("Parking Spot $spotNumber is already vacant")
            } else {
                vehicle = null
                isAvailable = true
                println("Vacating spot number: $spotNumber occupied by vehicle: ${vehicle?.getLicensePlateNumber()}")
            }
        }
    }

    fun getSpotSize(): VehicleSize {
        return spotType.size
    }
}

data class CompactSpot(
    override val spotNumber: String
) : ParkingSpot {
    override val spotType: ParkingSpotType
        get() = ParkingSpotType.COMPACT

    override var vehicle: Vehicle? = null
    override var isAvailable: Boolean = true
}

data class RegularSpot(
    override val spotNumber: String
) : ParkingSpot {
    override val spotType: ParkingSpotType
        get() = ParkingSpotType.REGULAR

    override var vehicle: Vehicle? = null
    override var isAvailable: Boolean = true
}

data class LargeSpot(
    override val spotNumber: String
) : ParkingSpot {
    override val spotType: ParkingSpotType
        get() = ParkingSpotType.LARGE

    override var vehicle: Vehicle? = null
    override var isAvailable: Boolean = true
}

data class Ticket(
    val ticketNumber: String,
    val parkingSpot: ParkingSpot,
    val vehicle: Vehicle,
    val entryTime: LocalDateTime,
    var exitTime: LocalDateTime? = null
) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateParkingDurationInHours(exitTime: LocalDateTime): Long {
        val duration = java.time.Duration.between(entryTime, exitTime)
        return duration.toHours()
    }

    companion object {
        private var ticketNum = 0
        fun createTicket(
            vehicle: Vehicle,
            parkingSpot: ParkingSpot,
            entryTime: LocalDateTime
        ): Ticket {
            return Ticket(ticketNumber = "ticket_" + ticketNum++, parkingSpot, vehicle, entryTime)
        }
    }
}

interface FareStrategy {
    fun calculateFare(ticket: Ticket, inputFare: Double): Double
}


class BaseFareStrategy : FareStrategy {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun calculateFare(ticket: Ticket, inputFare: Double): Double {

        val calculatedFare = when (ticket.vehicle.vehicleSize) {
            VehicleSize.SMALL -> {
                ticket.calculateParkingDurationInHours(
                    ticket.exitTime
                        ?: LocalDateTime.now()
                ) * SMALL_VEHICLE_RATE_PER_HOUR
            }

            VehicleSize.MEDIUM -> {
                ticket.calculateParkingDurationInHours(
                    ticket.exitTime
                        ?: LocalDateTime.now()
                ) * MEDIUM_VEHICLE_RATE_PER_HOUR
            }

            VehicleSize.LARGE -> {
                ticket.calculateParkingDurationInHours(
                    ticket.exitTime
                        ?: LocalDateTime.now()
                ) * LARGE_VEHICLE_RATE_PER_HOUR
            }
        }

        return inputFare + calculatedFare
    }

    companion object {
        private const val SMALL_VEHICLE_RATE_PER_HOUR = 10.0
        private const val MEDIUM_VEHICLE_RATE_PER_HOUR = 20.0
        private const val LARGE_VEHICLE_RATE_PER_HOUR = 30.0
    }
}

class WeekendDiscountFareStrategy : FareStrategy {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun calculateFare(ticket: Ticket, inputFare: Double): Double {
        val baseFare = inputFare

        val isWeekend = ticket.entryTime.dayOfWeek.name == "SATURDAY" ||
                ticket.entryTime.dayOfWeek.name == "SUNDAY"

        return if (isWeekend) {
            baseFare * 0.8 // 20% discount on weekends
        } else {
            baseFare
        }
    }
}

class NightTimeDiscountFareStrategy : FareStrategy {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun calculateFare(ticket: Ticket, inputFare: Double): Double {
        val baseFare = inputFare

        val entryHour = ticket.entryTime.hour
        val exitHour = ticket.exitTime?.hour ?: LocalDateTime.now().hour

        val isNightTime = (entryHour >= 22 || entryHour < 6) &&
                (exitHour >= 22 || exitHour < 6)

        return if (isNightTime) {
            baseFare * 0.9 // 10% discount for night time parking
        } else {
            baseFare
        }
    }
}

class PeakHourSurchargeFareStrategy : FareStrategy {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun calculateFare(ticket: Ticket, inputFare: Double): Double {
        val baseFare = inputFare

        val entryHour = ticket.entryTime.hour
        val exitHour = ticket.exitTime?.hour ?: LocalDateTime.now().hour

        val isPeakHour = (entryHour in 8..10 || entryHour in 17..19) ||
                (exitHour in 8..10 || exitHour in 17..19)

        return if (isPeakHour) {
            baseFare * 1.15 // 15% surcharge during peak hours
        } else {
            baseFare
        }
    }
}

class FareCalculator(
    private val fareStrategies: List<FareStrategy>
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateFare(ticket: Ticket): Double {
        var fare = FLAT_FAIR

        fareStrategies.forEach {
            fare = it.calculateFare(ticket, fare)
        }

        return fare
    }

    companion object {
        private const val FLAT_FAIR = 50.0
    }
}

enum class PaymentMethod {
    CASH, CREDIT_CARD, DEBIT_CARD, MOBILE_PAYMENT
}

data class Payment(
    val paymentId: String,
    val ticket: Ticket,
    val amount: Double,
    val method: PaymentMethod,
    val paymentTime: LocalDateTime
)


class ParkingLot(val parkingManager: ParkingManager, val fareCalculator: FareCalculator) {

    // Method to handle vehicle entry into the parking lot
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterVehicle(vehicle: Vehicle): Ticket? {
        // Delegate parking logic to ParkingManager
        val spot: ParkingSpot? = parkingManager.parkVehicle(vehicle)

        if (spot != null) {
            // Create ticket with entry time
            val ticket = Ticket.createTicket(vehicle, spot, LocalDateTime.now())
            return ticket
        } else {
            return null // No spot available
        }
    }

    // Method to handle vehicle exit from the parking lot
    @RequiresApi(Build.VERSION_CODES.O)
    fun leaveVehicle(ticket: Ticket?) {
        // Ensure the ticket is valid and the vehicle hasn't already left
        if (ticket != null && ticket.exitTime == null) {
            // Set exit time
            ticket.exitTime = LocalDateTime.now()

            // Delegate unparking logic to ParkingManager
            parkingManager.unparkVehicle(ticket.vehicle)

            // Calculate the fare
            val fare: Double = fareCalculator.calculateFare(ticket)
        } else {
            // Invalid ticket or vehicle already exited.
        }
    }
}

class ParkingManager(private val availableSpots: Map<VehicleSize, MutableList<ParkingSpot>>) {
    private val vehicleToSpotMap: MutableMap<Vehicle, ParkingSpot> = HashMap()
    private val spotToVehicleMap: MutableMap<ParkingSpot, Vehicle> = HashMap()

    private fun findSpotForVehicle(vehicle: Vehicle): ParkingSpot? {
        val vehicleSize = vehicle.vehicleSize

        val spotSizesToCheck = when (vehicleSize) {
            VehicleSize.SMALL -> listOf(
                VehicleSize.SMALL,
                VehicleSize.MEDIUM,
                VehicleSize.LARGE
            )

            VehicleSize.MEDIUM -> listOf(
                VehicleSize.MEDIUM,
                VehicleSize.LARGE
            )

            VehicleSize.LARGE -> listOf(
                VehicleSize.LARGE
            )
        }

        for (size in spotSizesToCheck) {
            val spots = availableSpots[size]
            if (!spots.isNullOrEmpty()) {
                //First available fit
                return spots.first()
            }
        }

        return null
    }

    fun parkVehicle(vehicle: Vehicle): ParkingSpot? {
        val spot = findSpotForVehicle(vehicle)
        if (spot != null) {
            spot.occupy(vehicle)
            // Record bidirectional mapping
            vehicleToSpotMap[vehicle] = spot
            spotToVehicleMap[spot] = vehicle
            // Remove the spot from the available list
            availableSpots[spot.getSpotSize()]!!.remove(spot)
            return spot // Parking successful
        }
        return null // No spot found for this vehicle
    }

    fun unparkVehicle(vehicle: Vehicle) {
        val spot = vehicleToSpotMap.remove(vehicle)
        if (spot != null) {
            spotToVehicleMap.remove(spot)
            spot.vacate()
            availableSpots[spot.getSpotSize()]!!.add(spot)
        }
    }

    // Find vehicle's parking spot
    fun findVehicleBySpot(vehicle: Vehicle): ParkingSpot? {
        return vehicleToSpotMap[vehicle]
    }

    // Find which vehicle is parked in a spot
    fun findSpotByVehicle(spot: ParkingSpot): Vehicle? {
        return spotToVehicleMap[spot]
    }
}






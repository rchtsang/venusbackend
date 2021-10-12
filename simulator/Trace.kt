package venusbackend.simulator

import venusbackend.div
import venusbackend.riscv.InstructionField
import venusbackend.riscv.MachineCode
import venusbackend.riscv.insts.dsl.types.Instruction
import venusbackend.simulator.Tracer.Companion.wordAddressed
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Created by Thaumic on 7/14/2018.
 */

class Trace(branched: Boolean, jumped: Boolean, ecallMsg: String, regs: Array<Number>, inst: MachineCode, line: Int, pc: Number, mem: Memory, error: SimulatorError? = null) {
    var branched = false
    var jumped = false
    var ecallMsg = ""
    var regs = Array<Number>(0) { 0 }
    var inst = MachineCode(0)
    var line = 0
    var pc: Number = 0
    var prevTrace: Trace? = null
    var error: SimulatorError? = null

    init {
        this.ecallMsg = ecallMsg
        this.branched = branched
        this.jumped = jumped
        this.regs = regs
        this.inst = inst
        this.line = line
        this.pc = pc
        this.mem = mem
        this.error = error
    }

    fun getString(format: String, base: Int): String {
        if (this.error != null) {
            return this.error.toString()
        }
        if (this.ecallMsg == "exiting the simulator") {
            return "exiting the simulator\n"
        }
        val code = try {
            Instruction[this.inst].disasm(this.inst)
        } catch (e: SimulatorError) {
            "Invalid Instruction"
        }
        var f = format.replace("%output%", this.ecallMsg)
                    .replace("%inst%", numToBase(base, this.inst.get(InstructionField.ENTIRE), this.inst.length * 8, true))
                    .replace("%pc%", numToBase(base, this.getPC(), 32, false))
                    .replace("%line%", numToBase(base, this.line, 16, false))
                    .replace("%decode%", code)
        for (i in 0..(regs.size - 1)) {
            f = f.replace("%" + i.toString() + "%", numToBase(base, this.regs[i].toInt(), 32, true))
            f = f.replace("%x" + i.toString() + "%", numToBase(base, this.regs[i].toInt(), 32, true))
        }
        val memregex = Regex("%mem:[a-f0-9A-F]+%")
        val matches = memregex.findAll(format)
        for (match in matches) {
        	val addr = (match.value.substring(5, match.value.length -1)).toLong(radix=16)
        	f.replace(match, numToBase(base, this.mem.loadWord(addr)), 32, false)
        }
        return f
    }

    fun getPC(): Number {
        if (wordAddressed) {
            return this.pc / 4
        }
        return this.pc
    }

    fun copy(): Trace {
        /*@fixme This is not a pure copy since modifing internal things in the copy still can affect the main.*/
        return Trace(branched, jumped, ecallMsg, regs.copyOf(), inst, line, pc)
    }
}
/*
* Takes in a base 10 integer and a base to convert it to and returns a string of what the number is.
*/
fun numToBase(curNumBase: Int, nu: Number, lengthNeeded: Int, signextend: Boolean): String {
    val n = nu as Int // FIXME
    val amount = ((2).toDouble()).pow(lengthNeeded.toDouble())
    val length = getBaseLog(curNumBase.toDouble(), amount).roundToInt()
    var num = if (signextend) {
        (decimalToHexString(n).toLong(16)).toString(curNumBase)
    } else {
        n.toString(curNumBase)
    }
    if (length - num.length > 0) {
        num = "0".repeat(length - num.length) + num
    }
    var snum = ""
    if (curNumBase == 2) {
        for (i in 0 until length) {
            if (i % 4 == 0 && i != 0) {
                snum += " "
            }
            snum += num[i]
        }
    } else {
        snum = num
    }
    return snum
}

fun getBaseLog(x: Double, y: Double): Double {
    return (log2(y) / log2(x))
}

fun decimalToHexString(number: Int): String {
    var retval = number.toLong()
    if (number < 0) {
        retval = 0xFFFFFFFF + number + 1
    }
    val rv = retval.toString(16).toUpperCase()
    return if (rv.length > 8) {
        rv.substring(0 until 8)
    } else {
        rv
    }
}
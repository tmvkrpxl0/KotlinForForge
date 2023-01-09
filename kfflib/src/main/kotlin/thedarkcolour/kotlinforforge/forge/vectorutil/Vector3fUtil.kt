package thedarkcolour.kotlinforforge.forge.vectorutil

import com.mojang.math.Vector3d
import com.mojang.math.Vector3f
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3

public operator fun Vector3f.plusAssign(other: Vector3f): Unit = add(other)

public operator fun Vector3f.plus(other: Vector3f): Vector3f {
    val newOne = Vector3f(x(), y(), z())
    newOne += other
    return newOne
}

public operator fun Vector3f.unaryMinus(): Vector3f = this * -1F

public operator fun Vector3f.minusAssign(other: Vector3f): Unit = sub(other)

public operator fun Vector3f.minus(other: Vector3f): Vector3f {
    val newOne = Vector3f(x(), y(), z())
    newOne -= other
    return newOne
}

public operator fun Vector3f.timesAssign(times: Float): Unit = mul(times)

public operator fun Vector3f.times(times: Float): Vector3f {
    val newOne = Vector3f(x(), y(), z())
    newOne *= times
    return newOne
}

public fun Vector3f.clone(): Vector3f {
    return Vector3f(x(), y(), z())
}

public fun Vector3f(other: Vec3): Vector3f {
    return Vector3f(other.x.toFloat(), other.y.toFloat(), other.z.toFloat())
}

public fun Vector3f(other: Vector3d): Vector3f {
    return Vector3f(other.x.toFloat(), other.y.toFloat(), other.z.toFloat())
}

public fun Vector3f(other: Vec3i): Vector3f {
    return Vector3f(other.x.toFloat(), other.y.toFloat(), other.z.toFloat())
}

public operator fun Vector3f.component1(): Float = x()

public operator fun Vector3f.component2(): Float = y()

public operator fun Vector3f.component3(): Float = z()

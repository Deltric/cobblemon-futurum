/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.api.storage.party.PartyPosition
import com.cobblemon.mod.common.api.storage.pc.PCPosition
import com.cobblemon.mod.common.net.IntSize
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import net.minecraft.nbt.EndTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.Tag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.Utf8String
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.StreamEncoder
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import java.io.IOException
import java.util.*

fun RegistryFriendlyByteBuf.readItemStack(): ItemStack {
    return ItemStack.OPTIONAL_STREAM_CODEC.decode(this)
}

fun RegistryFriendlyByteBuf.writeItemStack(itemStack: ItemStack) { // TODO (techdaan): why was itemStack optional?
    ItemStack.OPTIONAL_STREAM_CODEC.encode(this, itemStack)
}

fun RegistryFriendlyByteBuf.readText(): Component {
    return ComponentSerialization.STREAM_CODEC.decode(this)
}

fun RegistryFriendlyByteBuf.writeText(text: Component) { // TODO (techdaan): why was text optional?
    ComponentSerialization.STREAM_CODEC.encode(this, text)
}

fun ByteBuf.readEntityDimensions(): EntityDimensions {
    val width = readFloat()
    val height = readFloat()
    val isFixed = this.readBoolean()
    return if (isFixed) {
        EntityDimensions.fixed(width, height)
    }
    else {
        EntityDimensions.scalable(width, height)
    }
}


fun <B : ByteBuf, T> B.writeCollection(collection: Collection<T>, writer: (B, T) -> Unit) {
    this.writeInt(collection.size)
    collection.forEach {
        writer(this, it)
    }
}

// The removed FriendlyByteBuf helpers used VarInt counts. Keep these overloads
// separate from the raw ByteBuf helpers, whose existing format uses an Int.
fun <B : FriendlyByteBuf, T> B.writeCollection(collection: Collection<T>, writer: (B, T) -> Unit) {
    ByteBufCodecs.writeCount(this, collection.size, Int.MAX_VALUE)
    collection.forEach { writer(this, it) }
}

fun <B : FriendlyByteBuf, T, C : MutableCollection<T>> B.readCollection(
    factory: (Int) -> C,
    reader: (B) -> T
): C {
    val size = readCollectionSize(Int.MAX_VALUE)
    return factory(size).also { collection ->
        repeat(size) { collection.add(reader(this)) }
    }
}

fun <B : FriendlyByteBuf, T> B.readList(reader: (B) -> T): List<T> = readList(Int.MAX_VALUE, reader)

fun <B : FriendlyByteBuf, T> B.readList(maxSize: Int, reader: (B) -> T): List<T> {
    val size = readCollectionSize(maxSize)
    // Do not reserve an attacker-controlled capacity before decoding any entries.
    return ArrayList<T>(minOf(size, 65536)).also { list ->
        repeat(size) { list.add(reader(this)) }
    }
}

private fun FriendlyByteBuf.readCollectionSize(maxSize: Int): Int {
    require(maxSize >= 0) { "Maximum collection size must be non-negative" }
    val size = ByteBufCodecs.readCount(this, maxSize)
    if (size < 0) throw DecoderException("Negative collection size: $size")
    return size
}

fun <B : FriendlyByteBuf, K, V> B.writeMap(map: Map<K, V>, keyWriter: (B, K) -> Unit, valueWriter: (B, V) -> Unit) {
    ByteBufCodecs.writeCount(this, map.size, Int.MAX_VALUE)
    map.forEach { (key, value) ->
        keyWriter(this, key)
        valueWriter(this, value)
    }
}

fun <B : FriendlyByteBuf, K, V> B.readMap(keyReader: (B) -> K, valueReader: (B) -> V): Map<K, V> =
    readMap(Int.MAX_VALUE, keyReader, valueReader)

fun <B : FriendlyByteBuf, K, V> B.readMap(maxSize: Int, keyReader: (B) -> K, valueReader: (B) -> V): Map<K, V> {
    val size = readCollectionSize(maxSize)
    return HashMap<K, V>(minOf(size, 65536)).also { map ->
        repeat(size) {
            val key = keyReader(this)
            val value = valueReader(this)
            map[key] = value
        }
    }
}

fun FriendlyByteBuf.readVec3(): Vec3 = Vec3.STREAM_CODEC.decode(this)

fun FriendlyByteBuf.writeVec3(value: Vec3) = Vec3.STREAM_CODEC.encode(this, value)

fun <T> ByteBuf.writeNullable(obj: T?, writer: (ByteBuf, T) -> Unit) {
    this.writeBoolean(obj != null)
    obj?.let {
        writer(this, it)
    }
}

fun <T : Any> ByteBuf.writeNullable(obj: T?, writer: StreamEncoder<ByteBuf, T>) {
    writeNullable(obj) { buf, otherObj ->
        writer.encode(buf, otherObj)
    }
}

fun ByteBuf.writeString(string: String): ByteBuf {
    Utf8String.write(this, string, 32767)
    return this
}

fun ByteBuf.readString(): String {
    return Utf8String.read(this, 32767)
}

fun ByteBuf.writeExpression(expression: Expression): ByteBuf {
    return this.writeString(expression.getString())
}

fun ByteBuf.writeNullableExpression(expression: Expression?): ByteBuf {
    this.writeNullable(expression) { buf, expr -> buf.writeExpression(expr) }
    return this
}

fun ByteBuf.writeExpressionLike(expressionLike: ExpressionLike): ByteBuf {
    return this.writeString(expressionLike.getString())
}

fun ByteBuf.writeRidingStats(stats: Map<RidingStat, IntRange>) {
    return this.writeMap(
        stats,
        { buf, stat -> buf.writeEnumConstant(stat) },
        { buf, range -> buf.writeInt(range.first).writeInt(range.last) }
    )
}

fun ByteBuf.readRidingStats(): Map<RidingStat, IntRange> {
    return this.readMap(
        { buf -> buf.readEnumConstant(RidingStat::class.java) },
        { buf -> IntRange(buf.readInt(), buf.readInt()) }
    )
}

fun ByteBuf.readExpression(): Expression {
    return this.readString().asExpression()
}

fun ByteBuf.readNullableExpression(): Expression? {
    return this.readNullable { buf -> buf.readExpression() }
}

fun ByteBuf.readExpressionLike(): ExpressionLike {
    return this.readString().asExpressionLike()
}

fun <T> ByteBuf.readNullable(reader: (ByteBuf) -> T): T? {
    val isPresent = this.readBoolean()
    if (isPresent) {
        return reader.invoke(this)
    }
    return null
}

fun <B : ByteBuf, K, V> B.writeMap(map: Map<K, V>, keyWriter: (B, K) -> Unit, valueWriter: (B, V) -> Unit) {
    this.writeInt(map.size)
    map.forEach { (key, value) ->
        keyWriter(this, key)
        valueWriter(this, value)
    }
}

fun <B : ByteBuf, K, V> B.readMap(keyReader: (B) -> K, valueReader: (B) -> V): Map<K, V> {
    val map = mutableMapOf<K, V>()
    val numElements = readInt()
    repeat(numElements) {
        val key = keyReader.invoke(this)
        val value = valueReader.invoke(this)
        map[key] = value
    }
    return map
}


fun ByteBuf.readIdentifier(): Identifier {
    val str = this.readString()
    //If this is null we should be using writeNullable anyway
    return Identifier.tryParse(str)!!
}

fun ByteBuf.writeIdentifier(id: Identifier) {
    writeString(id.toString())
}

fun ByteBuf.writePartyPosition(partyPosition: PartyPosition) {
    writeSizedInt(IntSize.U_SHORT, partyPosition.slot)
}

fun ByteBuf.readPartyPosition() = PartyPosition(readSizedInt(IntSize.U_SHORT))

fun ByteBuf.writePCPosition(pcPosition: PCPosition) {
    writeSizedInt(IntSize.U_SHORT, pcPosition.box)
    writeSizedInt(IntSize.U_BYTE, pcPosition.slot)
}

fun ByteBuf.readPCPosition() = PCPosition(readSizedInt(IntSize.U_SHORT), readSizedInt(IntSize.U_BYTE))

fun ByteBuf.writeUUID(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun ByteBuf.readUUID() = UUID(readLong(), readLong())

fun ByteBuf.writeEnumConstant(value: Enum<*>) {
    this.writeInt(value.ordinal)
}

fun <T : Enum<T>> ByteBuf.readEnumConstant(clazz: Class<T>): T {
    return clazz.getEnumConstants()[this.readInt()]
}

fun ByteBuf.writeNbt(nbt: Tag) {
    if (nbt == null) {
        NbtIo.writeAnyTag(EndTag.INSTANCE, ByteBufOutputStream(this))
    }

    try {
        NbtIo.writeAnyTag(nbt, ByteBufOutputStream(this))
    } catch (var3: IOException) {
        val iOException = var3
        throw EncoderException(iOException)
    }
}

fun ByteBuf.readNbt(): Tag? {
    val iOException: IOException
    val nbtElement: Tag
    try {
        nbtElement = NbtIo.read(ByteBufInputStream(this), NbtAccounter.create(2097152L))
        if (nbtElement.type == EndTag.TYPE) { // TODO (techdaan): ensure this works
            return null
        }
    } catch (var4: IOException) {
        iOException = var4
        throw EncoderException(iOException)
    }

    try {
        return nbtElement
    } catch (var3: IOException) {
        iOException = var3
        throw EncoderException(iOException)
    }
}

fun <E : Enum<E>> ByteBuf.writeEnumSet(enumSet: EnumSet<E>, type: Class<E>) {
    val enums: Array<E> = type.enumConstants as Array<E>
    val bitSet = BitSet(enums.size)

    for (i in enums.indices) {
        bitSet[i] = enumSet.contains(enums[i])
    }

    this.writeBitSet(bitSet, enums.size)
}

fun <E : Enum<E>> ByteBuf.readEnumSet(type: Class<E>): EnumSet<E> {
    val enums: Array<E> = type.enumConstants
    val bitSet = this.readBitSet(enums.size)
    val enumSet = EnumSet.noneOf(type)

    for (i in enums.indices) {
        if (bitSet[i]) {
            enumSet.add(enums[i])
        }
    }

    return enumSet
}

fun ByteBuf.writeBitSet(bitSet: BitSet, size: Int) {
    if (bitSet.length() > size) {
        val var10002 = bitSet.length()
        throw EncoderException("BitSet is larger than expected size ($var10002>$size)")
    } else {
        val bs = bitSet.toByteArray()
        this.writeBytes(bs.copyOf(Mth.positiveCeilDiv(size, 8)))
    }
}

fun ByteBuf.readBitSet(size: Int): BitSet {
    val bs = ByteArray(Mth.positiveCeilDiv(size, 8))
    this.readBytes(bs)
    return BitSet.valueOf(bs)
}

fun ByteBuf.writeMatrix3f(matrix: Matrix3f) {
    this.writeFloat(matrix.m00)
    this.writeFloat(matrix.m01)
    this.writeFloat(matrix.m02)
    this.writeFloat(matrix.m10)
    this.writeFloat(matrix.m11)
    this.writeFloat(matrix.m12)
    this.writeFloat(matrix.m20)
    this.writeFloat(matrix.m21)
    this.writeFloat(matrix.m22)
}

fun ByteBuf.readMatrix3f(): Matrix3f {
    return Matrix3f(
        readFloat(), readFloat(), readFloat(),
        readFloat(), readFloat(), readFloat(),
        readFloat(), readFloat(), readFloat(),
    )
}

fun ByteBuf.writeMatrix4f(matrix: Matrix4f) {
    this.writeFloat(matrix.m00())
    this.writeFloat(matrix.m01())
    this.writeFloat(matrix.m02())
    this.writeFloat(matrix.m03())

    this.writeFloat(matrix.m10())
    this.writeFloat(matrix.m11())
    this.writeFloat(matrix.m12())
    this.writeFloat(matrix.m13())

    this.writeFloat(matrix.m20())
    this.writeFloat(matrix.m21())
    this.writeFloat(matrix.m22())
    this.writeFloat(matrix.m23())

    this.writeFloat(matrix.m30())
    this.writeFloat(matrix.m31())
    this.writeFloat(matrix.m32())
    this.writeFloat(matrix.m33())
}

fun ByteBuf.readMatrix4f(): Matrix4f {
    return Matrix4f(
        readFloat(), readFloat(), readFloat(), readFloat(),
        readFloat(), readFloat(), readFloat(), readFloat(),
        readFloat(), readFloat(), readFloat(), readFloat(),
        readFloat(), readFloat(), readFloat(), readFloat(),
    )
}

package xyz.handshot.fortify.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Bukkit
import org.bukkit.Location

class LocationTypeAdapter : TypeAdapter<Location>() {

    override fun write(out: JsonWriter, value: Location) {
        out.beginObject()
        out.name("x")
        out.value(value.x)
        out.name("y")
        out.value(value.y)
        out.name("z")
        out.value(value.z)
        out.name("world")
        out.value(value.world.name)
        out.endObject()
    }

    override fun read(input: JsonReader): Location {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var worldName = "world"

        input.beginObject()

        while (input.hasNext()) {
            when (input.nextName()) {
                "x" -> x = input.nextDouble()
                "y" -> y = input.nextDouble()
                "z" -> z = input.nextDouble()
                "world" -> worldName = input.nextString()
            }
        }

        input.endObject()

        val world = Bukkit.getWorld(worldName)
        return Location(world, x, y, z)
    }

}
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager

fun main() {
    val socket = DatagramSocket(6000)
    println("Arrancamos el servidor.....")
    //Iniciamos el servidor y ejecutamos la función sobreescrita run()
    Thread {
        Server(socket).run()
    }.start()
}

class Server(private val socket: DatagramSocket) : Runnable {

    private val tamanyoByte = 1024

    override fun run() {
        while (true) {

            val recibirData = ByteArray(tamanyoByte)
            val paqueteRecibido = DatagramPacket(recibirData, recibirData.size)
            socket.receive(paqueteRecibido)

            val direccionRecibida = paqueteRecibido.address
            val puertoRecibido = paqueteRecibido.port
            val mensaje = String(paqueteRecibido.data, 0, paqueteRecibido.length)

            println("Mensaje del cliente: $mensaje")

            // Cada mensaje se va a procesar en un hilo diferente
            Thread {
                procesarMensaje(direccionRecibida, puertoRecibido, mensaje)
            }.start()
        }
    }

    /*
    Envía la respuesta al cliente
     */
    private fun enviarRespuesta(direccionCliente: InetAddress, puertoCliente: Int, respuesta: String) {
        val enviarData = respuesta.toByteArray()
        val paqueteEnviar = DatagramPacket(enviarData, enviarData.size, direccionCliente, puertoCliente)
        socket.send(paqueteEnviar)

        println("Respuesta al cliente: $respuesta")
    }

    private fun obtenerConexionBD(): Connection {
        val URL = "jdbc:mysql://localhost:3306/personal"
        val USER = "root"
        val PASSWORD = "ce1914"
        return DriverManager.getConnection(URL, USER, PASSWORD)
    }


    private fun procesarMensaje(direccionRecibida: InetAddress, puertoRecibido: Int, mensaje: String) {
        // Valida la respuesta si es correcto registra al usuario sino devolverá instrucción no válida
        val respuesta = when {
            mensaje.startsWith("registro") -> registrarUsuario(mensaje)
            mensaje.startsWith("login") -> loginUsuario(mensaje)
            mensaje.startsWith("actualizar") -> actualizarUsuario(mensaje)
            else -> "Instrucción no válida"
        }
        // Enviar respuesta al cliente
        enviarRespuesta(direccionRecibida, puertoRecibido, respuesta)
    }

    private fun comprobarUsuario(email: String, pwd: String?, esRegistro: Boolean): Usuario? {
        return try {
            obtenerConexionBD().use { conexionBD ->
                val consultaSQL = if (esRegistro) {
                    "SELECT * FROM users WHERE email = ?"
                } else {
                    "SELECT * FROM users WHERE email = ? AND pwd = ?"
                }

                conexionBD.prepareStatement(consultaSQL).use { loginStatement ->
                    loginStatement.setString(1, email)

                    if (!esRegistro) {
                        loginStatement.setString(2, hashearPWD(pwd!!))
                    }

                    val resultado = loginStatement.executeQuery()

                    if (resultado.next()) {
                        Usuario(
                            resultado.getInt("id"),
                            resultado.getString("email"),
                            resultado.getString("pwd"),
                            resultado.getString("nombre"),
                            resultado.getString("prapellido"),
                            resultado.getString("sgapellido"),
                            resultado.getString("movil")
                        )
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


   /*
   Función que permite un login al cliente
    */
    private fun loginUsuario(mensaje: String): String {
        val cadena = mensaje.split(";")
        return if (cadena.size == 3) {
            val email = cadena[1]
            val pwd = cadena[2]

            val usuario = comprobarUsuario(email, pwd, false)

            if (usuario != null) {
                "${usuario.id},${usuario.email},${usuario.pwd}," +
                        "${usuario.nombre},${usuario.prapellido}," +
                        "${usuario.sgapellido},${usuario.movil}"
            } else {
                "Credenciales incorrectas."
            }
        } else {
            "Error al tramitar la solicitud."
        }
    }

   /*
   Función que permite registrar un usuario
    */
    private fun registrarUsuario(mensaje: String): String {
        val cadena = mensaje.split(";")
        return if (cadena.size == 3) {
            val email = cadena[1]
            val pwd = cadena[2]

            if (comprobarUsuario(email, null, true) == null) {
                try {
                    obtenerConexionBD().use { conexionBD ->
                        val consultaSQL = "INSERT INTO users (email, pwd) VALUES (?, ?)"
                        conexionBD.prepareStatement(consultaSQL).use { statement ->
                            statement.setString(1, email)
                            statement.setString(2, hashearPWD(pwd))
                            statement.executeUpdate()
                        }
                    }
                    "OK"
                } catch (e: Exception) {
                    e.printStackTrace()
                    "Error al registrar usuario."
                }
            } else {
                "Usuario ya existe"
            }
        } else {
            "Formato de mensaje incorrecto."
        }
    }
    /*
    Función que permite hashear la contraseña proporcionada por el usuario.
     */
    fun hashearPWD(texto:String): String{

        val sh512= MessageDigest.getInstance("sha-512")
        //Convertimos el texto en byte
        val textobyte= texto.toByteArray()
        val calcularhash= sh512.digest(textobyte)
        //Formato hexadecimal "%02x"
        val resultadoCorrecto= calcularhash.joinToString (""){"%02x".format(it) }
        return resultadoCorrecto
    }

    /*
    Función que permite actualizar el usuario según los datos pasados por el cliente
     */
    private fun actualizarUsuario(mensaje: String): String {
        val cadena = mensaje.split(";")

        return if (cadena.size == 6) {
            try {
                val userId = cadena[1].toInt()
                val nuevoNombre = cadena[2]
                val nuevoPrApellido = cadena[3]
                val nuevoSgApellido = cadena[4]
                val nuevoMovil = cadena[5]

                obtenerConexionBD().use { conexionBD ->
                    val consultaSQL = "update users set nombre=?, prapellido=?, sgapellido=?, movil=? WHERE id=?"
                    conexionBD.prepareStatement(consultaSQL).use { actualizarStatement ->
                        actualizarStatement.setString(1, nuevoNombre)
                        actualizarStatement.setString(2, nuevoPrApellido)
                        actualizarStatement.setString(3, nuevoSgApellido)
                        actualizarStatement.setString(4, nuevoMovil)
                        actualizarStatement.setInt(5, userId)

                        actualizarStatement.executeUpdate()
                    }
                }
                "OK"
            } catch (e: Exception) {
                e.printStackTrace()
                "Error al actualizar usuario."
            }
        } else {
            "Mensaje recibido incorrecto"
        }
    }

}

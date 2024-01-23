import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class Servidor(private val socket: DatagramSocket) : Runnable {

    private val tamanyoByte = 1024

    override fun run() {
        while (true) {

            val recibirData = ByteArray(tamanyoByte)
            val paqueteRecibido = DatagramPacket(recibirData, recibirData.size)
            socket.receive(paqueteRecibido)

            val direccionRecibida = paqueteRecibido.address
            val puertoRecibido = paqueteRecibido.port
            val mensaje = String(paqueteRecibido.data, 0, paqueteRecibido.length)

            // Cada mensaje se va a procesar en un hilo diferente
            Thread {
                procesarMensaje(direccionRecibida, puertoRecibido, mensaje)
            }.start()
        }
    }


    private fun enviarRespuesta(direccionCliente: InetAddress, puertoCliente: Int, respuesta: String) {
        val enviarData = respuesta.toByteArray()
        val paqueteEnviar = DatagramPacket(enviarData, enviarData.size, direccionCliente, puertoCliente)
        socket.send(paqueteEnviar)
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
            else -> "Instrucción no válida"
        }
        // Enviar respuesta al cliente
        enviarRespuesta(direccionRecibida, puertoRecibido, respuesta)
    }

    /*
    Función que nos permite comprobar si un usuario está o no registrado en la BBDD
     */

    private fun comprobarUsuario(email: String, pwd: String): Usuario? {
        // Establecemos conexión con la base de datos
        var conexionBD: Connection? = null
        var loginStatement: PreparedStatement? = null
        var resultado: ResultSet? = null

        try {
            //Establecemos la conexión con la base de datos
            conexionBD = obtenerConexionBD()

            // Verificamos si el correo electrónico y la contraseña coinciden
            loginStatement = conexionBD.prepareStatement(
                "select * from users where email=? and pwd=?"
            )
            loginStatement.setString(1, email)
            loginStatement.setString(2, pwd)
            resultado = loginStatement.executeQuery()

            if (resultado.next()) {
                //Si el usuario existe devolvemos el usuario
                return Usuario(
                    resultado.getInt("id"),
                    resultado.getString("email"),
                    resultado.getString("pwd"),
                    resultado.getString("nombre"),
                    resultado.getString("prapellido"),
                    resultado.getString("sgapellido"),
                    resultado.getString("movil")
                )
            } else {
                //En caso de que el usuario no exista, devolvemos un null
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                resultado?.close()
                loginStatement?.close()
                conexionBD?.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    /*
    Función que permite registrar un nuevo usuario en la BBDD
     */
    private fun registrarUsuario(mensaje: String): String {

        val cadena = mensaje.split(";")
        if (cadena.size == 3) {
            val email = cadena[1]
            val pwd = cadena[2]

            //Si al comprobar no existe procedemos a registrarlo
            if (comprobarUsuario(email, pwd) == null) {
                try {
                    //Establecemos conexión con la base de datos
                    val conexionBD = obtenerConexionBD()
                    val statement: PreparedStatement = conexionBD.prepareStatement(
                        "INSERT INTO users (email, pwd) VALUES (?, ?)"
                    )
                    statement.setString(1, email)
                    statement.setString(2, pwd)
                    statement.executeUpdate()  // insertamos los nuevos datos del usuario
                    statement.close()
                    conexionBD.close()
                    return "OK"
                } catch (e: Exception) {
                    e.printStackTrace()
                    return "Error al registrar usuario."
                }
            } else {
                return "Usuario ya existe"
            }

        } else {
            return "Formato de mensaje incorrecto."
        }
    }

    /*
    Función que permite recuperar los datos del usuario existente de la BBDD
     */
    private fun loginUsuario(mensaje: String): String {
        val cadena = mensaje.split(";")

        if (cadena.size == 3) {
            val email = cadena[1]
            val pwd = cadena[2]

            val usuario = comprobarUsuario(email, pwd)

            return if (usuario != null) {
                "${usuario.id},${usuario.email},${usuario.pwd}," +
                        "${usuario.nombre},${usuario.prapellido}," +
                        "${usuario.sgapellido},${usuario.movil}"
            } else {
                "Credenciales incorrectas."
            }
        } else {
            return "Error al tramitar la solicitud."
        }
    }
}

fun main() {
    val socket = DatagramSocket(6000)
    //Iniciamos el servidor y ejecutamos la función sobreescrita run()
    Thread {
        Servidor(socket).run()
    }.start()
}


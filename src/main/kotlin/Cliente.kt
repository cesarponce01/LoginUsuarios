import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*

fun main() {
    val sc = Scanner(System.`in`)
    var opcion: Int

    do {
        println("     ***   MENÚ DE OPCIONES   *** ")
        println("")
        println("      (  1  )  REGISTRO ")
        println("      (  2  )  LOGIN ")
        println("      ( -1  )  SALIR ")
        print("\nIngrese su opción: ")

        //Restringimos que sea un número
        opcion = if (sc.hasNextInt()) sc.nextInt() else -2
        sc.nextLine()  //Liberamos el buffer

        when (opcion) {
            1 -> {
                realizarRegistro()
            }

            2 -> {
                loginCliente()
            }

            -1 -> {
                println("Saliendo del programa...")
            }

            else -> {
                println("Opción no válida. Por favor, ingrese 1, 2 o -1.")
            }
        }
    } while (opcion != -1)
}

fun dataGrama(solicitud: String, email: String, pwd: String): String {
    val dataGramaEnviar = DatagramSocket()
    val mensaje = "$solicitud;$email;$pwd"
    val enviarMensaje = mensaje.toByteArray()

    val ip = InetAddress.getByName("127.0.0.1")
    val paqueteData = DatagramPacket(enviarMensaje, enviarMensaje.size, ip, 6000)
    dataGramaEnviar.send(paqueteData)

    //Recibimos respuesta del servidor
    val recibirData = ByteArray(1024)
    val paqueteRecibir = DatagramPacket(recibirData, recibirData.size)
    dataGramaEnviar.receive(paqueteRecibir)

    val texto = String(paqueteRecibir.data, 0, paqueteRecibir.length)
    //println("\nRespuesta recibida del servidor: $texto")
    return texto
}

fun realizarRegistro() {

    val sc = Scanner(System.`in`)
    println("************************************")
    println("*******  REGISTRO DE USUARIO *******")
    println("************************************")
    println("")

    println("Ingrese su email: ")
    val email = sc.nextLine()

    println("Ingrese su contraseña: ")
    val pwd = sc.nextLine()
    val texto = dataGrama("registro", email, pwd)

    if (texto == "OK") {
        println("*** Usuario registrado ***")
    } else {
        println("Mensaje del servidor: "+texto)
    }
}

fun loginCliente() {
    val sc = Scanner(System.`in`)
    println("*********************************")
    println("*******  LOGIN DE USUARIO *******")
    println("*********************************")
    println("")

    println("Ingrese su email: ")
    val email = sc.nextLine()

    println("Ingrese su contraseña: ")
    val pwd = sc.nextLine()

    val texto = dataGrama("login", email, pwd)

    //Se limita la visualización de la contraseña por *
    if (texto != "Credenciales incorrectas.") {
        val user = texto.split(",")

        println("Información del usuario:\n")
        println(
            "Id Usuario         : " + user[0] +
                    "\nEmail            : " + user[1] +
                    "\nContraseña       : " + "************"+
                    "\nNombre           : " + user[3] +
                    "\nPrimer Apellido  : " + user[4] +
                    "\nSegundo Apellido : " + user[5] +
                    "\nNúmero móvil     : " + user[6]
        )

        println("¿Desea actualizar sus datos? (SI/NO): ")
        if (sc.nextLine().uppercase(Locale.getDefault()) == "SI") {
            clienteActualizar(user[0].toInt())
        } else {
            println("\nMuchas gracias")
            println("***************************************")
            println("***************************************")
            println("")
        }

    } else {
        println("Mensaje del servidor: $texto")
    }
}

fun clienteActualizar(idUser: Int) {
    val sc = Scanner(System.`in`)

    // Solicitamos al cliente el resto de los datos
    println("\nRellene los siguientes datos:")
    print("Ingrese su Nombre: ")
    val nuevoNombre = sc.nextLine()

    print("Ingrese su Primer Apellido: ")
    val nuevoPrApellido = sc.nextLine()

    print("Ingrese su Segundo Apellido: ")
    val nuevoSgApellido = sc.nextLine()

    print("Ingrese su número de móvil : ")
    val nuevoMovil = sc.nextLine()

    //concatenamos los valores
    val cadenaEnvio = "$nuevoNombre;$nuevoPrApellido;$nuevoSgApellido;$nuevoMovil"

    // Validar el número de móvil
    if (validarMovil(nuevoMovil)) {
        // Preramos el envío al servidor con la solicitud de actualizar.
        val respuesta = dataGrama("actualizar", idUser.toString(), cadenaEnvio)

        // Mostrar respuesta del servidor
        println("Respuesta del servidor: $respuesta")
        println("***************************************")
        println("***************************************")
        println("")
    } else {
        println("Número de móvil incorrecto")
    }
}

fun validarMovil(movil: String): Boolean {
    val patronMovil = Regex("^[6-7]\\d{8}$")
    return patronMovil.matches(movil)
}



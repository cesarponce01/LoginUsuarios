import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*

fun dataGrama(solicitud:String, email:String, pwd: String):String{
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
    val texto=dataGrama("registro",email, pwd)

    if (texto == "OK") {
        println("Usuario registrado")
    } else {
        println("El usuario no se ha podido registrar")
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

    val texto=dataGrama("login",email, pwd)
    val user=texto.split(",")

    println("Información del usuario:")
    println("Id Usuario  : "+user[0]+
            "\nEmail     : "+user[1]+
            "\nContraseña: "+"*".repeat(user[2].length)+
            "\nNombre    : "+user[3]+
            "\nPrimer Apellido : "+user[4]+
            "\nSegundo Apellido: "+user[5]+
            "\nNúmero de móvil : "+user[6])
}

fun main() {
    val sc = Scanner(System.`in`)

    println("Opción a elegir (registro, login): ")
    val opcion = sc.nextLine()

    when (opcion.uppercase(Locale.getDefault())) {
        "REGISTRO" -> {
            realizarRegistro()
        }
        "LOGIN" -> {
            loginCliente()
        }
        else -> {
            println("Opción no válida")
        }
    }
}

# Domino Caos

Aplicación Android (Kotlin + Jetpack Compose) para jugar **"Domino Caos"** en línea. Los jugadores colocan fichas por turnos, usan poderes y el estado de la partida cambia con eventos aleatorios.

Taller 2 de *Programación por Componentes*, Universidad Distrital Francisco José de Caldas.

**Integrantes:** Kevin Garzon, María Vargas

## Características

- Registro e inicio de sesión con correo y contraseña (Firebase Authentication).
- Salas en línea con código de 5 caracteres, de 2 a 4 jugadores, con sincronización en tiempo real (Firebase Realtime Database).
- Modo offline contra un rival automático.
- 4 poderes con usos limitados: Comodín, Salto, Intercambio e Inversión.
- 4 eventos aleatorios: Tormenta de fichas, Puntos dobles, Turno perdido y Lluvia de puntos.
- Puntaje actualizado en cada jugada, máximo de rondas, desempate, abandono y reinicio de partida.
- Interfaz oscura y minimalista. El tablero se acomoda en serpiente y ajusta el tamaño de las fichas para que toda la cadena quepa sin desplazarse.

## Reglas del juego

- Cada jugador inicia con 7 fichas y la partida avanza por turnos.
- En cada turno se juega una ficha que coincida con un extremo del tablero. Si no hay jugada, se roba del pozo; si el pozo está vacío, se pierde el turno.
- Los poderes se eligen **antes** de mover:
    - **Comodín:** permite colocar una ficha sin cumplir un extremo.
    - **Salto:** el siguiente jugador pierde su turno.
    - **Intercambio:** cambia una ficha propia por una al azar de un rival.
    - **Inversión:** cambia el sentido de los turnos.
- **Puntaje:** cada ficha jugada suma sus puntos. Quien se queda sin fichas recibe un bonus con los puntos que sobran en las manos rivales.
- **Eventos:** tras cada turno puede ocurrir un evento (35 % de probabilidad, nunca dos seguidos). Afectan fichas, pozo, turno o puntaje.
- **Fin de la partida:** un jugador se queda sin fichas, nadie puede jugar o se completan las 10 rondas. Gana el mayor puntaje; en empate, quien usó menos poderes.
- Se puede abandonar (la partida termina para todos) y, al finalizar, reiniciar.

## Arquitectura (MVVM)

```
Pantallas (Compose)  ──►  ViewModels  ──►  Motor de juego (Kotlin puro)
  ui/screens                viewmodel          logic/engine
  ui/components                 │
                                └──►  Repositorios Firebase (data/)
```

- **Vista** (`ui/`): pantallas y componentes reutilizables. Solo dibujan el estado y reenvían los toques; no contienen reglas del juego.
- **ViewModel** (`viewmodel/`): mantiene el estado de la pantalla (`GameUiState`) y traduce las acciones del usuario en llamadas al motor y a los repositorios.
- **Modelo** (`logic/`): el motor de juego (`GameEngine`, `MoveValidator`, `PowerManager`, `RandomEvents`) son funciones puras: reciben un estado y devuelven el nuevo. No dependen de Android ni de Firebase, por eso se prueban con pruebas unitarias simples.
- **Datos** (`data/`): autenticación y persistencia/sincronización de partidas.

En línea, cada jugador aplica su jugada con el motor, guarda el nuevo estado en Firebase y los demás dispositivos lo reciben por un observador en tiempo real.

## Estructura del proyecto

```
app/src/main/java/com/caos/dominocaos/
├── MainActivity.kt
├── data/           FirebaseAuthRepository, FirebaseGameRepository
├── logic/
│   ├── model/      Tile, Player, GameState, GameEvent, PowerType
│   └── engine/     GameEngine, MoveValidator, PowerManager, RandomEvents, EventManager
├── viewmodel/      AuthViewModel, LobbyViewModel, RoomViewModel, GameViewModel, GameUiState
└── ui/
    ├── components/ DominoTileView, BoardView, PlayerHand, PlayerInfoBar, PowerButton, GameControls
    ├── screens/    auth, lobby, room, game
    ├── navigation/ AppNavigation
    └── theme/      Color, Theme, Type
```

## Cómo ejecutar

1. Clonar el repositorio y abrirlo con Android Studio (JDK 17 o superior, el que trae Android Studio).
2. Sincronizar Gradle y ejecutar en un emulador o dispositivo (Android 7.0 / API 24 o superior).
3. Para jugar en línea se necesitan dos dispositivos (o un emulador y un celular) con **cuentas distintas**.

El repositorio incluye el `google-services.json` del proyecto de Firebase del grupo. Si se quiere usar un proyecto propio:

1. Crear un proyecto en la [consola de Firebase](https://console.firebase.google.com) y agregar una app Android con el paquete `com.caos.dominocaos`.
2. Reemplazar `app/google-services.json` por el descargado.
3. En **Authentication > Sign-in method**, activar *Correo electrónico/contraseña*.
4. Crear una **Realtime Database** y publicar estas reglas:

```json
{
  "rules": {
    "games": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

## Cómo jugar

1. Registrarse o iniciar sesión.
2. **Crear partida** (aparece un código de 5 caracteres) o **Unirme** con el código de otro jugador.
3. El anfitrión pulsa **Iniciar partida** cuando haya al menos 2 jugadores.
4. Tocar una ficha para jugarla. Si cabe en ambos extremos, tocar el "+" del lado deseado.
5. Para usar un poder, tocarlo antes de mover (tocarlo de nuevo lo cancela).

También hay un modo **offline** contra un rival automático desde el lobby.

## Pruebas

```
./gradlew test
```

50 pruebas unitarias (JUnit 4) sobre validación de jugadas, puntaje, desempate, rondas, abandono, poderes, eventos aleatorios y lógica del estado de la interfaz.

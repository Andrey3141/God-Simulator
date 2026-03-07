## PROJECT_STRUCTURE.md

# Структура проекта God Simulator

```
GodSimulator/
├── app/                           # Главный модуль приложения
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/printer/godsimulator/  # Kotlin исходники
│   │   │   │   ├── MainActivity.kt              # Точка входа
│   │   │   │   ├── GameView.kt                  # Игровой движок (SurfaceView)
│   │   │   │   ├── World.kt                     # Модель мира (чанки, тайлы)
│   │   │   │   ├── GameConfig.kt                # Конфигурация игры ⭐
│   │   │   │   ├── WorldSave.kt                 # Система сохранений ⭐ НОВОЕ
│   │   │   │   ├── Chicken.kt                   # Курицы (спрайтовая анимация)
│   │   │   │   ├── Creature.kt                  # Существa (процедурная отрисовка)
│   │   │   │   ├── Animation.kt                 # Анимация кадров
│   │   │   │   ├── SpriteManager.kt             # Загрузка спрайтов
│   │   │   │   └── PixelMan.kt                  # Пиксельный человек (устарел)
│   │   │   ├── res/                             # Ресурсы
│   │   │   │   ├── drawable/                     # Изображения
│   │   │   │   │   ├── chicken_walk.png         # Спрайт курицы (4×4)
│   │   │   │   │   └── grass.png                # Текстура травы
│   │   │   │   ├── layout/                       # Макеты
│   │   │   │   │   └── activity_main.xml        # (зарезервировано)
│   │   │   │   ├── values/                       # Стили и строки
│   │   │   │   ├── xml/                          # Правила бэкапа
│   │   │   │   └── mipmap/                       # Иконки
│   │   │   └── AndroidManifest.xml               # Манифест
│   │   └── test/                                 # Модульные тесты
│   ├── build.gradle.kts                          # Модульные настройки
│   └── proguard-rules.pro                        # Правила обфускации
├── gradle/                                        # Gradle обертка
│   ├── wrapper/
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml                        # Версии зависимостей
├── build.gradle.kts                              # Корневые настройки
├── settings.gradle.kts                            # Настройки проекта
├── gradle.properties                              # Свойства Gradle
├── gradlew                                        # Linux/Mac скрипт
├── gradlew.bat                                    # Windows скрипт
├── CHANGELOG.md                                   # История изменений ⭐ ОБНОВЛЕНО
├── PROJECT_STRUCTURE.md                           # Этот файл ⭐ ОБНОВЛЕНО
└── README.md                                      # Документация проекта
```

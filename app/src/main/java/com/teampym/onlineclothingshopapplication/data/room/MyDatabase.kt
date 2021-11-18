package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(
    entities = [
        Region::class, Province::class, City::class,
        UserInformation::class, DeliveryInformation::class, NotificationToken::class,
        Cart::class, Product::class, Inventory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MyDatabase : RoomDatabase() {

    abstract fun regionDao(): RegionDao
    abstract fun provinceDao(): ProvinceDao
    abstract fun cityDao(): CityDao

    abstract fun userInformationDao(): UserInformationDao
    abstract fun deliveryInformationDao(): DeliveryInformationDao
    abstract fun notificationTokenDao(): NotificationTokenDao

    abstract fun cartDao(): CartDao
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao

    class Callback @Inject constructor(
        private val database: Provider<MyDatabase>,
        @ApplicationScope private val appScope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val regionDao = database.get().regionDao()
            val provinceDao = database.get().provinceDao()
            val cityDao = database.get().cityDao()

            appScope.launch {
                regionDao.insert(Region(1, "Metro Manila"))
                regionDao.insert(Region(2, "Mindanao"))
                regionDao.insert(Region(3, "North Luzon"))
                regionDao.insert(Region(4, "South Luzon"))
                regionDao.insert(Region(5, "Visayas"))

                // Metro Manila
                provinceDao.insert(Province(1, 1, "Metro Manila"))
                // Cities of Metro Manila
                cityDao.insert(City(1, 1, "Binondo"))
                cityDao.insert(City(2, 1, "Caloocan City"))
                cityDao.insert(City(3, 1, "Ermita"))
                cityDao.insert(City(4, 1, "Intramuros"))
                cityDao.insert(City(5, 1, "Las Pinas City"))
                cityDao.insert(City(6, 1, "Makati City"))
                cityDao.insert(City(7, 1, "Malabon City"))
                cityDao.insert(City(8, 1, "Malate"))
                cityDao.insert(City(9, 1, "Marikina City"))
                cityDao.insert(City(10, 1, "Muntinlupa City"))
                cityDao.insert(City(11, 1, "Navotas City"))
                cityDao.insert(City(12, 1, "Paco"))
                cityDao.insert(City(13, 1, "Pandacan"))
                cityDao.insert(City(14, 1, "Paranaque City"))
                cityDao.insert(City(15, 1, "Pasay City"))
                cityDao.insert(City(16, 1, "Pasig City"))
                cityDao.insert(City(17, 1, "Pateros"))
                cityDao.insert(City(18, 1, "Port Area"))
                cityDao.insert(City(19, 1, "Quezon City"))
                cityDao.insert(City(20, 1, "Quiapo"))
                cityDao.insert(City(21, 1, "Sampaloc"))
                cityDao.insert(City(22, 1, "San Juan City"))
                cityDao.insert(City(23, 1, "San Miguel"))
                cityDao.insert(City(24, 1, "San Nicolas"))
                cityDao.insert(City(25, 1, "Santa Ana"))
                cityDao.insert(City(26, 1, "Santa Cruz"))
                cityDao.insert(City(27, 1, "Taguig City"))
                cityDao.insert(City(28, 1, "Valenzuela City"))

                // Mindanao
                // Agusan Del Norte
                provinceDao.insert(Province(2, 2, "Agusan Del Norte"))
                // Cities
                cityDao.insert(City(29, 2, "Buenavista"))
                cityDao.insert(City(30, 2, "Butuan City"))
                cityDao.insert(City(31, 2, "Cabadbaran City"))
                cityDao.insert(City(32, 2, "Carmen"))
                cityDao.insert(City(33, 2, "Jabonga"))
                cityDao.insert(City(34, 2, "Kitcharao"))
                cityDao.insert(City(35, 2, "Las Nieves"))
                cityDao.insert(City(36, 2, "Magallanes"))
                cityDao.insert(City(37, 2, "Nasipit"))
                cityDao.insert(City(38, 2, "Remedios T. Romualdez"))
                cityDao.insert(City(39, 2, "Santiago"))
                cityDao.insert(City(40, 2, "Tubay"))

                // Agusan Del Sur
                provinceDao.insert(Province(3, 2, "Agusan Del Sur"))
                // Cities
                cityDao.insert(City(41, 3, "Bayugan City"))
                cityDao.insert(City(42, 3, "Bunawan"))
                cityDao.insert(City(43, 3, "Esperanza"))
                cityDao.insert(City(44, 3, "La Paz"))
                cityDao.insert(City(45, 3, "Loreto"))
                cityDao.insert(City(46, 3, "Properidad"))
                cityDao.insert(City(47, 3, "Rosario"))
                cityDao.insert(City(48, 3, "San Francisco"))
                cityDao.insert(City(49, 3, "San Luis"))
                cityDao.insert(City(50, 3, "Santa Josefa"))
                cityDao.insert(City(51, 3, "Sibagat"))
                cityDao.insert(City(52, 3, "Talacogon"))
                cityDao.insert(City(53, 3, "Trento"))
                cityDao.insert(City(54, 3, "Veruela"))

                // Basilan
                provinceDao.insert(Province(4, 2, "Basilan"))
                // Cities
                cityDao.insert(City(55, 4, "Akbar"))
                cityDao.insert(City(56, 4, "Al-Barka"))
                cityDao.insert(City(57, 4, "Hadji Mohammad Ajul"))
                cityDao.insert(City(58, 4, "Hadju Muhtamad"))
                cityDao.insert(City(59, 4, "Isabela City"))
                cityDao.insert(City(60, 4, "Lamitan City"))
                cityDao.insert(City(61, 4, "Lantawan"))
                cityDao.insert(City(62, 4, "Maluso"))
                cityDao.insert(City(63, 4, "Sumisip"))
                cityDao.insert(City(64, 4, "Tabuan-Lasa"))
                cityDao.insert(City(65, 4, "Tipo-Tipo"))
                cityDao.insert(City(66, 4, "Tuburan"))
                cityDao.insert(City(67, 4, "Ungkaya Pukan"))

                // North Luzon
                // Abra
                provinceDao.insert(Province(5, 3, "Abra"))
                // Cities
                cityDao.insert(City(68, 5, "Bangued"))
                cityDao.insert(City(69, 5, "Boliney"))
                cityDao.insert(City(70, 5, "Bucay"))
                cityDao.insert(City(71, 5, "Bucloc"))
                cityDao.insert(City(72, 5, "Daguioman"))
                cityDao.insert(City(73, 5, "Danglas"))
                cityDao.insert(City(74, 5, "Dolores"))
                cityDao.insert(City(75, 5, "La Paz"))
                cityDao.insert(City(76, 5, "Lacub"))
                cityDao.insert(City(77, 5, "Lagalingan"))
                cityDao.insert(City(78, 5, "Lagayan"))
                cityDao.insert(City(79, 5, "Langiden"))
                cityDao.insert(City(80, 5, "Licuan (Baay)"))
                cityDao.insert(City(81, 5, "Luba"))
                cityDao.insert(City(82, 5, "Malibcong"))
                cityDao.insert(City(83, 5, "Manabo"))
                cityDao.insert(City(84, 5, "Penarrubia"))
                cityDao.insert(City(85, 5, "Pidigan"))
                cityDao.insert(City(86, 5, "Pilar"))
                cityDao.insert(City(87, 5, "Sallapadan"))
                cityDao.insert(City(88, 5, "San Isidro"))
                cityDao.insert(City(89, 5, "San Juan"))
                cityDao.insert(City(90, 5, "San Quintin"))
                cityDao.insert(City(91, 5, "Tayum"))
                cityDao.insert(City(92, 5, "Tineg"))
                cityDao.insert(City(93, 5, "Tubo"))
                cityDao.insert(City(94, 5, "Villaciosa"))

                // Apayao
                provinceDao.insert(Province(6, 3, "Apayao"))
                // Cities
                cityDao.insert(City(95, 6, "Calanasan"))
                cityDao.insert(City(96, 6, "Conner"))
                cityDao.insert(City(97, 6, "Flora"))
                cityDao.insert(City(98, 6, "Kabugao"))
                cityDao.insert(City(99, 6, "Luna"))
                cityDao.insert(City(100, 6, "Pudtol"))
                cityDao.insert(City(101, 6, "Santa Marcela"))

                // Aurora
                provinceDao.insert(Province(7, 3, "Aurora"))
                // Cities
                cityDao.insert(City(102, 7, "Baler"))
                cityDao.insert(City(103, 7, "Casiguran"))
                cityDao.insert(City(104, 7, "Dilasag"))
                cityDao.insert(City(105, 7, "Dinalungan"))
                cityDao.insert(City(106, 7, "Dingalan"))
                cityDao.insert(City(107, 7, "Dipaculao"))
                cityDao.insert(City(108, 7, "Maria Aurora"))
                cityDao.insert(City(109, 7, "San Luis"))

                // South Luzon
                // Albay
                provinceDao.insert(Province(8, 4, "Albay"))
                // Cities
                cityDao.insert(City(110, 8, "Bacacay"))
                cityDao.insert(City(111, 8, "Camalig"))
                cityDao.insert(City(112, 8, "Daraga"))
                cityDao.insert(City(113, 8, "Guinobatan"))
                cityDao.insert(City(114, 8, "Jovellar"))
                cityDao.insert(City(115, 8, "Legazpi City"))
                cityDao.insert(City(116, 8, "Libon"))
                cityDao.insert(City(117, 8, "Ligao City"))
                cityDao.insert(City(118, 8, "Malilipot"))
                cityDao.insert(City(119, 8, "Malinao"))
                cityDao.insert(City(120, 8, "Manito"))
                cityDao.insert(City(121, 8, "Oas"))
                cityDao.insert(City(122, 8, "Pio Duran"))
                cityDao.insert(City(123, 8, "Polangui"))
                cityDao.insert(City(124, 8, "Rapu-Rapu"))
                cityDao.insert(City(125, 8, "Santo Domingo"))
                cityDao.insert(City(126, 8, "Tabaco City"))
                cityDao.insert(City(127, 8, "Tiwi"))

                // Batangas
                provinceDao.insert(Province(9, 4, "Batangas"))
                // Cities
                cityDao.insert(City(128, 9, "Agoncillo"))
                cityDao.insert(City(129, 9, "Alitagtag"))
                cityDao.insert(City(130, 9, "Balayan"))
                cityDao.insert(City(131, 9, "Balete"))
                cityDao.insert(City(132, 9, "Batangas City"))
                cityDao.insert(City(133, 9, "Bauan"))
                cityDao.insert(City(134, 9, "Calaca"))
                cityDao.insert(City(135, 9, "Calatagan"))
                cityDao.insert(City(136, 9, "Cuenca"))
                cityDao.insert(City(137, 9, "Ibaan"))
                cityDao.insert(City(138, 9, "Laurel"))
                cityDao.insert(City(139, 9, "Lemery"))
                cityDao.insert(City(140, 9, "Lian"))
                cityDao.insert(City(141, 9, "Lipa City"))
                cityDao.insert(City(142, 9, "Lobo"))
                cityDao.insert(City(143, 9, "Mabini"))
                cityDao.insert(City(144, 9, "Malvar"))
                cityDao.insert(City(145, 9, "Mataasnakahoy"))
                cityDao.insert(City(146, 9, "Nasugbu"))
                cityDao.insert(City(147, 9, "Padre Garcia"))
                cityDao.insert(City(148, 9, "Rosario"))
                cityDao.insert(City(149, 9, "San Jose"))
                cityDao.insert(City(150, 9, "San Luis"))
                cityDao.insert(City(151, 9, "San Nicolas"))
                cityDao.insert(City(152, 9, "San Pascual"))
                cityDao.insert(City(153, 9, "Santa Teresita"))
                cityDao.insert(City(154, 9, "Santo Tomas"))
                cityDao.insert(City(155, 9, "Taal"))
                cityDao.insert(City(156, 9, "Talisay"))
                cityDao.insert(City(157, 9, "Tanauan City"))
                cityDao.insert(City(158, 9, "Taysan"))
                cityDao.insert(City(159, 9, "Tingloy"))
                cityDao.insert(City(160, 9, "Tuy"))

                // Catanduanes
                provinceDao.insert(Province(10, 4, "Catanduanes"))
                // Cities
                cityDao.insert(City(161, 10, "Baras"))
                cityDao.insert(City(162, 10, "Bato"))
                cityDao.insert(City(163, 10, "Bayamonoc"))
                cityDao.insert(City(164, 10, "Caramoran"))
                cityDao.insert(City(165, 10, "Gigmoto"))
                cityDao.insert(City(166, 10, "Panaan"))
                cityDao.insert(City(167, 10, "Panganiban"))
                cityDao.insert(City(168, 10, "San Andres"))
                cityDao.insert(City(169, 10, "San Miguel"))
                cityDao.insert(City(170, 10, "Viga"))
                cityDao.insert(City(171, 10, "Virac"))

                // Visayas
                // Aklan
                provinceDao.insert(Province(11, 5, "Aklan"))
                // Cities
                cityDao.insert(City(172, 11, "Altavas"))
                cityDao.insert(City(173, 11, "Balete"))
                cityDao.insert(City(174, 11, "Banga"))
                cityDao.insert(City(175, 11, "Batan"))
                cityDao.insert(City(176, 11, "Buruanga"))
                cityDao.insert(City(177, 11, "Ibajay"))
                cityDao.insert(City(178, 11, "Kalibo"))
                cityDao.insert(City(179, 11, "Lezo"))
                cityDao.insert(City(180, 11, "Libacao"))
                cityDao.insert(City(181, 11, "Madalag"))
                cityDao.insert(City(182, 11, "Makato"))
                cityDao.insert(City(183, 11, "Malay"))
                cityDao.insert(City(184, 11, "Malinao"))
                cityDao.insert(City(185, 11, "Nabas"))
                cityDao.insert(City(186, 11, "New Washington"))
                cityDao.insert(City(187, 11, "Numancia"))
                cityDao.insert(City(188, 11, "Tangalan"))

                // Antique
                provinceDao.insert(Province(12, 5, "Antique"))
                // Cities
                cityDao.insert(City(189, 12, "Anini-Y"))
                cityDao.insert(City(190, 12, "Barbaza"))
                cityDao.insert(City(191, 12, "Belison"))
                cityDao.insert(City(192, 12, "Bugasong"))
                cityDao.insert(City(193, 12, "Caluya"))
                cityDao.insert(City(194, 12, "Culasi"))
                cityDao.insert(City(195, 12, "Hamtic"))
                cityDao.insert(City(196, 12, "Laua-an"))
                cityDao.insert(City(197, 12, "Libertad"))
                cityDao.insert(City(198, 12, "Pandan"))
                cityDao.insert(City(199, 12, "Patnongon"))
                cityDao.insert(City(200, 12, "San Jose"))
                cityDao.insert(City(201, 12, "San Remigio"))
                cityDao.insert(City(202, 12, "Sebaste"))
                cityDao.insert(City(203, 12, "Sibalom"))
                cityDao.insert(City(204, 12, "Tibiao"))
                cityDao.insert(City(205, 12, "Tobias Fornier"))
                cityDao.insert(City(206, 12, "Valderrama"))

                // Bohol
                provinceDao.insert(Province(13, 5, "Bohol"))
                // Cities
                cityDao.insert(City(207, 13, "Alburquerque"))
                cityDao.insert(City(208, 13, "Alicia"))
                cityDao.insert(City(209, 13, "Anda"))
                cityDao.insert(City(210, 13, "Antequera"))
                cityDao.insert(City(211, 13, "Baclayon"))
                cityDao.insert(City(212, 13, "Balilihan"))
                cityDao.insert(City(213, 13, "Batuan"))
                cityDao.insert(City(214, 13, "Bien Unido"))
                cityDao.insert(City(215, 13, "Bilar"))
                cityDao.insert(City(216, 13, "Buenavista"))
                cityDao.insert(City(217, 13, "Calape"))
                cityDao.insert(City(218, 13, "Candijay"))
                cityDao.insert(City(219, 13, "Carmen"))
                cityDao.insert(City(220, 13, "Catigbian"))
                cityDao.insert(City(221, 13, "Clarin"))
                cityDao.insert(City(222, 13, "Corella"))
                cityDao.insert(City(223, 13, "Cortes"))
                cityDao.insert(City(224, 13, "Dagohoy"))
                cityDao.insert(City(225, 13, "Danao"))
                cityDao.insert(City(226, 13, "Dauis"))
                cityDao.insert(City(227, 13, "Dimiao"))
                cityDao.insert(City(228, 13, "Duero"))
                cityDao.insert(City(229, 13, "Garcia Hernandez"))
                cityDao.insert(City(230, 13, "Guindulman"))
                cityDao.insert(City(231, 13, "Inabanga"))
                cityDao.insert(City(232, 13, "Jagna"))
                cityDao.insert(City(234, 13, "Jetafe"))
                cityDao.insert(City(235, 13, "Lila"))
                cityDao.insert(City(236, 13, "Loay"))
                cityDao.insert(City(237, 13, "Loboc"))
                cityDao.insert(City(238, 13, "Loon"))
                cityDao.insert(City(239, 13, "Mabini"))
                cityDao.insert(City(240, 13, "Maribojoc"))
                cityDao.insert(City(241, 13, "Panglao"))
                cityDao.insert(City(242, 13, "Pilar"))
                cityDao.insert(City(243, 13, "Pres. Carlos P. Garcia"))
                cityDao.insert(City(244, 13, "Sagbayan"))
                cityDao.insert(City(245, 13, "San Isidro"))
                cityDao.insert(City(246, 13, "San Miguel"))
                cityDao.insert(City(247, 13, "Sevilla"))
                cityDao.insert(City(248, 13, "Sierra Bullones"))
                cityDao.insert(City(249, 13, "Sikatuna"))
                cityDao.insert(City(250, 13, "Tagbilaran City"))
                cityDao.insert(City(251, 13, "Talibon"))
                cityDao.insert(City(252, 13, "Trinidad"))
                cityDao.insert(City(253, 13, "Tubigon"))
                cityDao.insert(City(254, 13, "Ubay"))
                cityDao.insert(City(255, 13, "Valencia"))
            }
        }
    }
}

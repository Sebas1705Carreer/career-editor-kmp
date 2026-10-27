package dev.sebas1705.careereditor

/**
 * Trimmed copies of what https://api.sebas1705.dev answered on 2026-10-25. Each entity keeps
 * its real shape (keys, null fields, localized maps); long lists were cut to a few items.
 */
object ApiFixtures {

    val languages = """
        {"default":"en","supported":[
          {"code":"en","label":"English","label_native":"English"},
          {"code":"es","label":"Spanish","label_native":"Español"}
        ]}
    """.trimIndent()

    val personal = """
        {"name":"Sebastián Entrerrios García","email":"dev@example.com",
         "cv_url":"https://sebas1705carreer.github.io/carreerV2/cv/cv-en.pdf",
         "github":"https://github.com/Sebas1705","linkedin":"https://www.linkedin.com/in/example/",
         "codewars":"https://www.codewars.com/users/Sebas1705",
         "greeting":{"en":"Hi, I'm","es":"Hola, soy"},
         "role":{"en":"Full Stack & Mobile Developer","es":"Desarrollador Full Stack & Móvil"},
         "tagline":{"en":"Full stack and mobile engineer.","es":"Ingeniero full stack y móvil."},
         "bio":{"en":"Based in Madrid, Spain.","es":"Con base en Madrid, España."},
         "location":{"en":"Madrid, Spain","es":"Madrid, España"}}
    """.trimIndent()

    /** A current job: endDate is null, achievements is a localized list. */
    val job = """
        {"id":"senior-solusoft","company":"Solusoft","companyUrl":"https://www.solusoft.es/",
         "startDate":"2026-01","endDate":null,
         "role":{"en":"Senior Full Stack & Mobile Developer","es":"Desarrollador Full Stack & Móvil Senior"},
         "type":{"en":"Hybrid","es":"Híbrido"},
         "period":{"en":"January 2026 - Present","es":"Enero 2026 - Presente"},
         "desc":{"en":"Leading development and architectural decisions.","es":"Liderazgo técnico y de decisiones arquitectónicas."},
         "projects":["AGEDI","Sisley","Iberext"],
         "achievements":{"en":["Promoted to Senior within the same team","Led architectural refactors"],
                         "es":["Promoción a Senior dentro del mismo equipo","Lideré refactorizaciones arquitectónicas"]},
         "long_desc":{"en":"Technical lead of the mobile and full-stack side.","es":"Lead técnico de la parte móvil y full-stack."}}
    """.trimIndent()

    /** A work project without public links: github and demo are explicit nulls. */
    val project = """
        {"id":"agedi","name":"AGEDI System","context":"work",
         "desc":{"en":"Enterprise management platform for AGEDI.","es":"Plataforma de gestión empresarial para AGEDI."},
         "tags":["C#",".NET","SQL Server"],"github":null,"demo":null,
         "long_desc":{"en":"Full-stack enterprise management platform.","es":"Plataforma empresarial full-stack."}}
    """.trimIndent()

    /** A project with the optional cover image (absolute https URL hosted by the portfolio + localized alt). */
    val projectWithImage = """
        {"id":"youknow","name":"YouKnow","context":"academic",
         "desc":{"en":"Quiz app.","es":"App de preguntas."},"tags":["Kotlin"],"github":null,"demo":null,
         "image_url":"https://sebas1705carreer.github.io/carreerV2/projects/youknow.webp",
         "image_alt":{"es":"Pantalla de inicio de YouKnow","en":"YouKnow home screen"}}
    """.trimIndent()

    val skill = """
        {"id":"kotlin","name":"Kotlin","level":4,"category":"languages",
         "icon_url":"https://img.icons8.com/?size=100&id=ZoxjA0jZDdFZ&format=png&color=000000"}
    """.trimIndent()

    val skillWithoutIcon = """{"id":"kmp","name":"Kotlin Multiplatform","level":2,"category":"frameworks","icon_url":null}"""

    val education = """
        {"id":"big-school","school":"BigSchool & Isabel I University","icon":"graduation",
         "degree":{"en":"Master's in AI Development","es":"Máster de Desarrollo con IA"},
         "period":{"en":"October 2025 - June 2026","es":"Octubre 2025 - Junio 2026"},
         "detail":{"en":"11-module postgraduate.","es":"Posgrado de 11 módulos."},
         "long_desc":{"en":"Intensive postgraduate programme.","es":"Posgrado intensivo."}}
    """.trimIndent()

    val certification = """
        {"id":"jetpack-compose-appcademy","name":"Jetpack Compose: Definitive Course","issuer":"AppCademy",
         "date":"Dec 2025","url":"https://www.appcademy.dev/certificates/cert_JGzFpajl",
         "long_desc":{"en":"Deep dive into declarative UI on Android.","es":"Inmersión en UI declarativa en Android."}}
    """.trimIndent()

    val certificationInProgress = """{"id":"aws-saa","name":"AWS Solutions Architect","issuer":"AWS","date":"2026","url":null,"long_desc":{}}"""

    val softSkill = """{"id":"adaptability","name":{"en":"Adaptability","es":"Adaptabilidad"}}"""

    /** GET /all: every entity in one object; note the hyphenated "soft-skills" key. */
    val all = """
        {"languages":$languages,
         "personal":$personal,
         "jobs":[$job],
         "projects":[$project],
         "skills":[$skill,$skillWithoutIcon],
         "education":[$education],
         "certifications":[$certification,$certificationInProgress],
         "soft-skills":[$softSkill]}
    """.trimIndent()

    // Error bodies the worker answers with (career-api-worker: auth.ts, router.ts, validation)
    val unauthorized = """{"error":"Unauthorized"}"""
    val validationFailed = """{"error":"Validation failed","details":["projects.context: must be one of: work, academic, personal","skills.level: must be between 1 and 4"]}"""
    val bodyTooLarge = """{"error":"Body too large"}"""
}

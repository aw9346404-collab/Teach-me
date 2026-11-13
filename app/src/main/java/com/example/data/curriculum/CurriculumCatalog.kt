package com.example.data.curriculum

import com.example.data.model.BadgeInfo
import com.example.data.model.DailyArticle
import com.example.data.model.ErrorSeverity
import com.example.data.model.LearnerProfile
import com.example.data.model.LearningPathStep
import com.example.data.model.QuizQuestion
import com.example.data.model.VocabWord

object CurriculumCatalog {

    val dailyArticles: List<DailyArticle> = listOf(
        DailyArticle(
            id = "art_slow_reading",
            titleEn = "The Quiet Power of Deliberate Reading",
            titleAr = "القوة الهادئة للقراءة المتأنية",
            cefrLevel = "B1",
            category = "Culture & Study",
            readingMinutes = 4,
            summaryAr = "مقال يوضح كيف تساعد القراءة المتأنية في ترسيخ التراكيب الإنكليزية والمفردات السياقية.",
            paragraphs = listOf(
                "In an era of rapid notifications and endless scrolling, deliberate reading has become a rare cognitive habit. When learners slow down to notice how clauses connect, they begin to absorb grammar naturally rather than memorizing isolated rules.",
                "Research in second-language acquisition shows that encountering a B1 or B2 word in three distinct contexts builds stronger recall than repeating a flashcard twenty times. Pay attention to collocations—words that frequently travel together, such as 'make progress' or 'draw a conclusion'.",
                "Try reading each paragraph aloud once after listening to its rhythm. Even two minutes of mindful reading each evening gradually transforms passive recognition into confident, active fluency."
            ),
            keyPhrases = listOf(
                "deliberate reading" to "القراءة المتأنية المقصودة",
                "draw a conclusion" to "استخلاص نتيجة",
                "passive recognition" to "الفهم الاستقبالي غير النشط"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_sr_1",
                    promptEn = "According to the article, what helps learners absorb grammar naturally?",
                    promptAr = "وفقاً للمقال، ما الذي يساعد المتعلمين على استيعاب القواعد بشكل طبيعي؟",
                    options = listOf(
                        "Slowing down to notice how clauses connect",
                        "Memorizing isolated grammar tables without context",
                        "Scrolling rapidly through short headlines",
                        "Translating every word literally"
                    ),
                    correctIndex = 0,
                    errorSeverityIfWrong = ErrorSeverity.RED_CRITICAL,
                    explanationAr = "ذكر المقال أن التمهّل وملاحظة كيفية ترابط الجمل يساعدان على اكتساب القواعد طبيعياً.",
                    explanationEn = "The passage states that slowing down to notice how clauses connect builds natural grammar.",
                    category = "READING"
                ),
                QuizQuestion(
                    id = "q_sr_2",
                    promptEn = "Which collocation is highlighted in the second paragraph?",
                    promptAr = "ما المتلازمة اللفظية (collocation) المذكورة في الفقرة الثانية؟",
                    options = listOf(
                        "do progress",
                        "make progress",
                        "create a conclusion",
                        "take a recognition"
                    ),
                    correctIndex = 1,
                    errorSeverityIfWrong = ErrorSeverity.ORANGE_MINOR,
                    explanationAr = "في الإنكليزية نستخدم 'make progress' وليس 'do progress'—خطأ شائع طفيف في اختيار الفعل المرافق.",
                    explanationEn = "'Make progress' is the natural collocation; 'do progress' is an unnatural pairing.",
                    category = "WORD_CHOICE"
                )
            )
        ),
        DailyArticle(
            id = "art_urban_forests",
            titleEn = "How Urban Forests Cool Modern Cities",
            titleAr = "كيف تبرّد الغابات الحضرية المدن الحديثة",
            cefrLevel = "B1+",
            category = "Environment",
            readingMinutes = 5,
            summaryAr = "استكشاف حلول التشجير الذكية في المدن وتأثيرها في خفض الحرارة وتحسين الصحة النفسية.",
            paragraphs = listOf(
                "Across Europe and the Middle East, city planners are rethinking concrete avenues by planting dense micro-forests along transit corridors. Unlike decorative lawns, layered canopies trap dust, absorb noise, and lower street temperatures by up to four degrees Celsius.",
                "Furthermore, accessible green spaces encourage residents to walk rather than rely on short car journeys. When neighborhoods feel calmer and more shaded, daily commutes turn into restorative routines instead of sources of fatigue.",
                "Although maintaining urban biodiversity requires steady municipal investment, the long-term savings in public health and energy demand far outweigh the initial cost."
            ),
            keyPhrases = listOf(
                "outweigh the initial cost" to "يفوق التكلفة الأولية",
                "restorative routines" to "عادات يومية مريحة ومجددة للطاقة",
                "transit corridors" to "ممرات النقل والمواصلات"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_uf_1",
                    promptEn = "What does the phrase 'far outweigh' mean in the final paragraph?",
                    promptAr = "ماذا تعني عبارة 'far outweigh' في الفقرة الأخيرة؟",
                    options = listOf(
                        "Are much heavier physically",
                        "Are significantly greater in value than",
                        "Are slightly less expensive than",
                        "Are delayed until next year"
                    ),
                    correctIndex = 1,
                    errorSeverityIfWrong = ErrorSeverity.RED_CRITICAL,
                    explanationAr = "تعني 'outweigh' أن الفوائد تفوق أو تتغلب على التكاليف من حيث القيمة والأهمية.",
                    explanationEn = "'Outweigh' means to be greater or more significant in value than something else.",
                    category = "WORD_CHOICE"
                ),
                QuizQuestion(
                    id = "q_uf_2",
                    promptEn = "Choose the grammatically accurate B1+ connector: '___ urban forests need maintenance, their benefits are substantial.'",
                    promptAr = "اختر أداة الربط الدقيقة: '___ تحتاج الغابات الحضرية إلى صيانة، فإن فوائدها كبيرة.'",
                    options = listOf("Despite", "Although", "Because of", "During"),
                    correctIndex = 1,
                    errorSeverityIfWrong = ErrorSeverity.RED_CRITICAL,
                    explanationAr = "تتبع 'Although' جملة كاملة (فاعل + فعل)، بينما تتبع 'Despite' عبارة اسمية.",
                    explanationEn = "'Although' introduces a full clause (subject + verb), whereas 'Despite' takes a noun phrase.",
                    category = "GRAMMAR"
                )
            )
        ),
        DailyArticle(
            id = "art_remote_collaboration",
            titleEn = "Navigating Nuance in International Teams",
            titleAr = "إتقان اللباقة والدقة في فرق العمل الدولية",
            cefrLevel = "B2",
            category = "Communication",
            readingMinutes = 5,
            summaryAr = "مهارات مستوى B2 للتعبير الدبلوماسي وطرح الآراء المخالفة باحترافية في الاجتماعات والرسائل.",
            paragraphs = listOf(
                "Transitioning from B1 to B2 English often hinges not on knowing rarer words, but on mastering diplomatic nuance. In international workplaces, phrasing a disagreement bluntly can unintentionally signal impatience.",
                "Instead of saying 'That plan will not work,' seasoned communicators soften their stance with hedging expressions: 'I see the appeal of that approach; however, we might run into timeline constraints.' Notice how the second sentence preserves collaboration while still raising a clear concern.",
                "Mastering modal verbs such as 'might', 'could', and 'would' enables you to sound thoughtful, persuasive, and culturally adaptable in both spoken discussions and formal emails."
            ),
            keyPhrases = listOf(
                "hinges on" to "يتوقف على / يعتمد جوهرياً على",
                "hedging expressions" to "عبارات التخفيف الدبلوماسي",
                "run into constraints" to "مواجهة قيود أو عقبات"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_rc_1",
                    promptEn = "Why do B2 speakers use hedging expressions like 'might' or 'could'?",
                    promptAr = "لماذا يستخدم متحدثو مستوى B2 عبارات التخفيف مثل 'might' و'could'؟",
                    options = listOf(
                        "To avoid answering the question completely",
                        "To raise concerns diplomatically while preserving collaboration",
                        "Because they forgot the simple present tense",
                        "To make emails longer"
                    ),
                    correctIndex = 1,
                    errorSeverityIfWrong = ErrorSeverity.RED_CRITICAL,
                    explanationAr = "تُستخدم عبارات التخفيف لطرح الملاحظات بأسلوب مهني ومهذب يحافظ على روح التعاون.",
                    explanationEn = "Hedging softens disagreement so concerns stay constructive and collaborative.",
                    category = "WORD_CHOICE"
                ),
                QuizQuestion(
                    id = "q_rc_2",
                    promptEn = "Which option is polite yet slightly informal (minor tone adjustment for formal B2 writing)?",
                    promptAr = "أي خيار مقبول المعنى لكنه يميل للعامية قليلاً مقارنة بأسلوب B2 الرسمي؟",
                    options = listOf(
                        "We may encounter scheduling challenges.",
                        "We might run into a few timeline snags.",
                        "That is completely wrong.",
                        "Timeline is impossible."
                    ),
                    correctIndex = 1,
                    errorSeverityIfWrong = ErrorSeverity.ORANGE_MINOR,
                    explanationAr = "'Timeline snags' صحيحة المعنى ومفهومة، لكنها أقل رسمية قليلاً من 'scheduling challenges' (ملاحظة برتقالية).",
                    explanationEn = "'Timeline snags' conveys the right meaning naturally but is slightly informal for strict academic/executive prose.",
                    category = "WORD_CHOICE"
                )
            )
        ),
        DailyArticle(
            id = "art_memory_sleep",
            titleEn = "Why Sleep Consolidates Vocabulary Overnight",
            titleAr = "لماذا يثبّت النوم المفردات الجديدة ليلاً",
            cefrLevel = "B2",
            category = "Science & Mind",
            readingMinutes = 4,
            summaryAr = "دراسة مبسطة حول دور النوم العميق في نقل المفردات الإنكليزية من الذاكرة المؤقتة إلى الدائمة.",
            paragraphs = listOf(
                "Have you ever struggled to recall an English idiom in the evening, only to find it rolls off your tongue effortlessly the following morning? Neuroscientists attribute this phenomenon to memory consolidation during deep sleep.",
                "While we rest, the hippocampus replays newly encoded linguistic patterns and transfers them to long-term cortical networks. Reviewing three or four challenging mistakes briefly before bedtime gives the brain clear material to strengthen overnight.",
                "Consequently, consistent fifteen-minute study sessions spread across the week dramatically outperform frantic last-minute cramming."
            ),
            keyPhrases = listOf(
                "rolls off your tongue" to "ينساب على لسانك بسلاسة",
                "attribute this to" to "يعزو هذا إلى",
                "dramatically outperform" to "يتفوق بشكل ملحوظ على"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_ms_1",
                    promptEn = "Complete the sentence accurately: 'Scientists ___ better recall ___ deep sleep consolidation.'",
                    promptAr = "أكمل الجملة بدقة: 'يعزو العلماء تحسن التذكر إلى تثبيت الذاكرة أثناء النوم العميق.'",
                    options = listOf(
                        "attribute / to",
                        "attribute / for",
                        "contribute / at",
                        "blame / with"
                    ),
                    correctIndex = 0,
                    errorSeverityIfWrong = ErrorSeverity.RED_CRITICAL,
                    explanationAr = "الفعل الصحيح هو 'attribute X to Y' بمعنى يعزو X إلى Y.",
                    explanationEn = "The correct verb pattern is 'attribute [effect] to [cause]'.",
                    category = "GRAMMAR"
                )
            )
        )
    )

    val vocabularyLibrary: List<VocabWord> = listOf(
        VocabWord(
            id = "v_coherent",
            word = "coherent",
            ipa = "/kəʊˈhɪə.rənt/",
            partOfSpeech = "adjective",
            cefrLevel = "B2",
            definitionEn = "Logical, clear, and well-organized so ideas connect smoothly.",
            translations = mapOf(
                "ar" to "مترابط / واضح ومنطقي",
                "fr" to "cohérent",
                "es" to "coherente",
                "tr" to "tutarlı",
                "de" to "zusammenhängend"
            ),
            exampleEn = "She presented a coherent argument during the seminar.",
            collocations = listOf("coherent argument", "coherent strategy", "remain coherent")
        ),
        VocabWord(
            id = "v_mitigate",
            word = "mitigate",
            ipa = "/ˈmɪt.ɪ.ɡeɪt/",
            partOfSpeech = "verb",
            cefrLevel = "B2",
            definitionEn = "To make something harmful, unpleasant, or severe less intense.",
            translations = mapOf(
                "ar" to "يخفف من حدة / يقلل أثر",
                "fr" to "atténuer",
                "es" to "mitigar",
                "tr" to "hafifletmek",
                "de" to "abmildern"
            ),
            exampleEn = "Planting street trees helps mitigate extreme summer heat.",
            collocations = listOf("mitigate risk", "mitigate the impact", "mitigate effects")
        ),
        VocabWord(
            id = "v_reluctant",
            word = "reluctant",
            ipa = "/rɪˈlʌk.tənt/",
            partOfSpeech = "adjective",
            cefrLevel = "B1+",
            definitionEn = "Unwilling or hesitant to do something.",
            translations = mapOf(
                "ar" to "متردد / غير راغب",
                "fr" to "réticent",
                "es" to "reacio",
                "tr" to "isteksiz",
                "de" to "widerwillig"
            ),
            exampleEn = "Many learners feel reluctant to speak aloud at first.",
            collocations = listOf("reluctant to admit", "seem reluctant", "reluctant agreement")
        ),
        VocabWord(
            id = "v_consequently",
            word = "consequently",
            ipa = "/ˈkɒn.sɪ.kwənt.li/",
            partOfSpeech = "adverb",
            cefrLevel = "B2",
            definitionEn = "As a direct result or effect of what was just mentioned.",
            translations = mapOf(
                "ar" to "وبالتالي / نتيجة لذلك",
                "fr" to "par conséquent",
                "es" to "por consiguiente",
                "tr" to "sonuç olarak",
                "de" to "folglich"
            ),
            exampleEn = "He reviewed his notes daily; consequently, his writing improved.",
            collocations = listOf("consequently led to", "and consequently")
        ),
        VocabWord(
            id = "v_substantial",
            word = "substantial",
            ipa = "/səbˈstæn.ʃəl/",
            partOfSpeech = "adjective",
            cefrLevel = "B2",
            definitionEn = "Large in size, value, or importance.",
            translations = mapOf(
                "ar" to "كبير / جوهري وملموس",
                "fr" to "substantiel",
                "es" to "sustancial",
                "tr" to "önemli ölçüde",
                "de" to "erheblich"
            ),
            exampleEn = "There has been a substantial improvement in her listening score.",
            collocations = listOf("substantial progress", "substantial evidence", "substantial amount")
        ),
        VocabWord(
            id = "v_reliable",
            word = "reliable",
            ipa = "/rɪˈlaɪ.ə.bəl/",
            partOfSpeech = "adjective",
            cefrLevel = "B1",
            definitionEn = "Consistently good in quality or able to be trusted.",
            translations = mapOf(
                "ar" to "موثوق / يُعتمد عليه",
                "fr" to "fiable",
                "es" to "confiable",
                "tr" to "güvenilir",
                "de" to "zuverlässig"
            ),
            exampleEn = "Consistent practice is the most reliable path to fluency.",
            collocations = listOf("reliable source", "reliable method", "prove reliable")
        ),
        VocabWord(
            id = "v_emphasize",
            word = "emphasize",
            ipa = "/ˈem.fə.saɪz/",
            partOfSpeech = "verb",
            cefrLevel = "B1+",
            definitionEn = "To give special importance or attention to something in speaking or writing.",
            translations = mapOf(
                "ar" to "يؤكد على / يبرز أهمية",
                "fr" to "souligner",
                "es" to "enfatizar",
                "tr" to "vurgulamak",
                "de" to "betonen"
            ),
            exampleEn = "The tutor emphasized the difference between critical and minor errors.",
            collocations = listOf("emphasize the importance", "strongly emphasize")
        ),
        VocabWord(
            id = "v_persuasive",
            word = "persuasive",
            ipa = "/pəˈsweɪ.sɪv/",
            partOfSpeech = "adjective",
            cefrLevel = "B2",
            definitionEn = "Good at convincing someone to agree with an idea or take action.",
            translations = mapOf(
                "ar" to "مُقنع",
                "fr" to "persuasif",
                "es" to "persuasivo",
                "tr" to "ikna edici",
                "de" to "überzeugend"
            ),
            exampleEn = "Using real examples makes your essay far more persuasive.",
            collocations = listOf("persuasive essay", "persuasive tone", "highly persuasive")
        ),
        VocabWord(
            id = "v_nuance",
            word = "nuance",
            ipa = "/ˈnjuː.ɑːns/",
            partOfSpeech = "noun",
            cefrLevel = "B2",
            definitionEn = "A subtle difference in meaning, tone, or expression.",
            translations = mapOf(
                "ar" to "فارق دقيق في المعنى أو النبرة",
                "fr" to "nuance",
                "es" to "matiz",
                "tr" to "ince fark",
                "de" to "Nuance"
            ),
            exampleEn = "Understanding emotional nuance separates B2 speakers from B1 learners.",
            collocations = listOf("subtle nuance", "cultural nuance", "capture the nuance")
        ),
        VocabWord(
            id = "v_gradually",
            word = "gradually",
            ipa = "/ˈɡrædʒ.u.ə.li/",
            partOfSpeech = "adverb",
            cefrLevel = "B1",
            definitionEn = "Slowly over a period of time rather than suddenly.",
            translations = mapOf(
                "ar" to "تدريجياً",
                "fr" to "progressivement",
                "es" to "gradualmente",
                "tr" to "yavaş yavaş",
                "de" to "allmählich"
            ),
            exampleEn = "Her pronunciation gradually became clearer over four weeks.",
            collocations = listOf("gradually improve", "gradually shift", "gradually increase")
        ),
        VocabWord(
            id = "v_adaptable",
            word = "adaptable",
            ipa = "/əˈdæp.tə.bəl/",
            partOfSpeech = "adjective",
            cefrLevel = "B2",
            definitionEn = "Able to adjust easily to new conditions or situations.",
            translations = mapOf(
                "ar" to "مرن / قابل للتكيف",
                "fr" to "adaptable",
                "es" to "adaptable",
                "tr" to "uyumlu",
                "de" to "anpassungsfähig"
            ),
            exampleEn = "Adaptable learners thrive when conversing on unfamiliar topics.",
            collocations = listOf("highly adaptable", "remain adaptable")
        ),
        VocabWord(
            id = "v_acquire",
            word = "acquire",
            ipa = "/əˈkwaɪər/",
            partOfSpeech = "verb",
            cefrLevel = "B1+",
            definitionEn = "To gain a skill, habit, or knowledge through effort and practice.",
            translations = mapOf(
                "ar" to "يكتسب",
                "fr" to "acquérir",
                "es" to "adquirir",
                "tr" to "edinmek",
                "de" to "erwerben"
            ),
            exampleEn = "Listening daily helps you acquire natural phrasing.",
            collocations = listOf("acquire fluency", "acquire vocabulary", "acquire skills")
        ),
        VocabWord(
            id = "v_feasible",
            word = "feasible",
            ipa = "/ˈfiː.zə.bəl/",
            partOfSpeech = "adjective",
            cefrLevel = "B2",
            definitionEn = "Possible and practical to achieve easily or conveniently.",
            translations = mapOf(
                "ar" to "عملي / قابل للتنفيذ",
                "fr" to "faisable",
                "es" to "factible",
                "tr" to "uygulanabilir",
                "de" to "machbar"
            ),
            exampleEn = "Studying fifteen minutes every morning is a feasible routine.",
            collocations = listOf("economically feasible", "feasible plan", "seem feasible")
        ),
        VocabWord(
            id = "v_clarify",
            word = "clarify",
            ipa = "/ˈklær.ɪ.faɪ/",
            partOfSpeech = "verb",
            cefrLevel = "B1+",
            definitionEn = "To make an idea or statement easier to understand.",
            translations = mapOf(
                "ar" to "يوضّح",
                "fr" to "clarifier",
                "es" to "aclarar",
                "tr" to "açıklığa kavuşturmak",
                "de" to "klären"
            ),
            exampleEn = "Could you clarify what you meant by that phrase?",
            collocations = listOf("clarify a point", "help clarify", "clarify expectations")
        ),
        VocabWord(
            id = "v_outcome",
            word = "outcome",
            ipa = "/ˈaʊt.kʌm/",
            partOfSpeech = "noun",
            cefrLevel = "B1+",
            definitionEn = "The final result or consequence of an activity or situation.",
            translations = mapOf(
                "ar" to "النتيجة النهائية / المحصّلة",
                "fr" to "résultat",
                "es" to "resultado",
                "tr" to "sonuç",
                "de" to "Ergebnis"
            ),
            exampleEn = "Careful preparation improves the outcome of any interview.",
            collocations = listOf("positive outcome", "likely outcome", "learning outcome")
        ),
        VocabWord(
            id = "v_elaborate",
            word = "elaborate",
            ipa = "/ɪˈlæb.ə.reɪt/",
            partOfSpeech = "verb",
            cefrLevel = "B2",
            definitionEn = "To add more detail or explain something more fully.",
            translations = mapOf(
                "ar" to "يفصّل / يشرح بتوسع",
                "fr" to "développer",
                "es" to "desarrollar",
                "tr" to "detaylandırmak",
                "de" to "ausführen"
            ),
            exampleEn = "Please elaborate on why you chose that solution.",
            collocations = listOf("elaborate on", "elaborate further")
        )
    )

    fun buildPersonalizedPath(profile: LearnerProfile): List<LearningPathStep> {
        val weakestSkill = listOf(
            "SPEAKING" to profile.speakingScore,
            "LISTENING" to profile.listeningScore,
            "GRAMMAR" to profile.grammarScore,
            "READING" to profile.readingScore,
            "VOCABULARY" to profile.vocabularyScore
        ).minByOrNull { it.second }?.first ?: "READING"

        val steps = listOf(
            LearningPathStep(
                id = "path_b1_article",
                stage = "B1",
                titleAr = "القراءة السمعية: مقال اليوم التفاعلي",
                titleEn = "Audio-Visual Daily Article",
                subtitleAr = "استمع واقرأ بتأنٍ ثم اجتز اختبار الفهم",
                subtitleEn = "Listen, read calmly, and pass the comprehension quiz",
                skillTag = "READING",
                xpReward = 45,
                targetTab = "articles",
                isCompleted = profile.articlesRead >= 1,
                isRecommendedNext = profile.articlesRead == 0L || weakestSkill == "READING" || weakestSkill == "LISTENING"
            ),
            LearningPathStep(
                id = "path_voice_studio",
                stage = profile.cefrLevel,
                titleAr = "جلسة النطق والتصحيح الفوري",
                titleEn = "AI Voice & Pronunciation Clinic",
                subtitleAr = "تحدث بالإنكليزية واحصل على تلوين فوري للأخطاء (أحمر/برتقالي)",
                subtitleEn = "Speak aloud for instant Red/Orange feedback",
                skillTag = "SPEAKING",
                xpReward = 60,
                targetTab = "voice",
                isCompleted = profile.voiceSessionsCount >= 1,
                isRecommendedNext = weakestSkill == "SPEAKING" && profile.articlesRead >= 1
            ),
            LearningPathStep(
                id = "path_vocab_b2",
                stage = "B1+",
                titleAr = "مكتبة المفردات الذكية (B1–B2)",
                titleEn = "Common Words & Collocations",
                subtitleAr = "استكشف الكلمات الشائعة بالبحث الذكي والنطق السليم",
                subtitleEn = "Explore high-frequency B1-B2 words with smart search",
                skillTag = "VOCABULARY",
                xpReward = 35,
                targetTab = "library",
                isCompleted = profile.vocabularyScore >= 70,
                isRecommendedNext = weakestSkill == "VOCABULARY"
            ),
            LearningPathStep(
                id = "path_mistake_review",
                stage = "B2",
                titleAr = "تحويل الأخطاء إلى إتقان",
                titleEn = "Mistake Bank Mastery",
                subtitleAr = "راجع أخطاءك الحمراء والبرتقالية المحفوظة وثبّت الصواب",
                subtitleEn = "Review saved red & orange errors to eliminate repeats",
                skillTag = "GRAMMAR",
                xpReward = 50,
                targetTab = "mistakes",
                isCompleted = profile.quizzesCompleted >= 3 && profile.grammarScore >= 75,
                isRecommendedNext = weakestSkill == "GRAMMAR"
            )
        )
        val hasRecommended = steps.any { it.isRecommendedNext }
        return if (hasRecommended) steps else steps.mapIndexed { idx, s ->
            s.copy(isRecommendedNext = idx == 0)
        }
    }

    fun evaluateBadges(profile: LearnerProfile): List<BadgeInfo> {
        val earned = profile.earnedBadges.toSet()
        return listOf(
            BadgeInfo(
                id = "first_step",
                titleAr = "الخطوة الأولى",
                titleEn = "First Ink",
                descriptionAr = "بدء المسار التعليمي من B1 إلى B2",
                descriptionEn = "Started your B1 to B2 journey",
                earned = "first_step" in earned || profile.xp >= 10
            ),
            BadgeInfo(
                id = "avid_reader",
                titleAr = "قارئ متأمل",
                titleEn = "Calm Reader",
                descriptionAr = "إتمام 3 مقالات يومية مع الاختبارات",
                descriptionEn = "Completed 3 daily articles & quizzes",
                earned = "avid_reader" in earned || profile.articlesRead >= 3
            ),
            BadgeInfo(
                id = "eloquent_voice",
                titleAr = "صوت واثق",
                titleEn = "Eloquent Voice",
                descriptionAr = "إكمال 3 جلسات نطق ومحادثة ذكية",
                descriptionEn = "Completed 3 AI voice evaluation sessions",
                earned = "eloquent_voice" in earned || profile.voiceSessionsCount >= 3
            ),
            BadgeInfo(
                id = "streak_7",
                titleAr = "مثابرة أسبوعية",
                titleEn = "Seven-Day Sage",
                descriptionAr = "الحفاظ على سلسلة تعلم لـ 7 أيام",
                descriptionEn = "Maintained a 7-day study streak",
                earned = "streak_7" in earned || profile.streakDays >= 7
            ),
            BadgeInfo(
                id = "b2_vanguard",
                titleAr = "طليعة مستوى B2",
                titleEn = "B2 Vanguard",
                descriptionAr = "الوصول إلى جاهزية مستوى B2",
                descriptionEn = "Reached B2 proficiency stage",
                earned = "b2_vanguard" in earned || profile.cefrLevel == "B2"
            )
        )
    }
}

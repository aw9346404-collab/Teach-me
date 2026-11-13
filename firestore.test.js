const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate() {
  return new Date(Date.now() - 2000);
}

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "Alice Learner",
    cefrLevel: "B1",
    learningGoal: "Reach B2 fluency in 90 days",
    xp: 120,
    streakDays: 3,
    articlesRead: 2,
    quizzesCompleted: 2,
    voiceSessionsCount: 1,
    readingScore: 68,
    listeningScore: 64,
    speakingScore: 60,
    grammarScore: 65,
    vocabularyScore: 70,
    preferredLang: "ar",
    translationLang: "ar",
    readingTheme: "WARM_PAPER",
    reminderEnabled: true,
    reminderHour: 19,
    reminderMinute: 30,
    earnedBadges: ["first_step"],
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validMistake(uid) {
  return {
    userId: uid,
    sourceType: "VOICE",
    originalText: "I am agree with your point.",
    correctedText: "I agree with your point.",
    errorSpan: "am agree",
    severity: "RED_CRITICAL",
    category: "GRAMMAR",
    explanationAr: "لا نستخدم verb to be قبل الفعل agree",
    explanationEn: "Do not use 'am' before the main verb 'agree'.",
    mastered: false,
    reviewCount: 0,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles or leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("mistakes").get());
  await assertFails(unauthDb.collection("leaderboard").get());
});

test("Authenticated owner can create and update their profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertSucceeds(ref.update({ xp: 200, updatedAt: pastDate() }));
});

test("Cross-user access to private profile and mistakes is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("mistakes").doc("m1").set(validMistake(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("mistakes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ xp: 9999, updatedAt: pastDate() }));
});

test("Shadow update with ghost field or invalid severity is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ isAdmin: true, updatedAt: pastDate() })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("mistakes").doc("m_bad").set({
      ...validMistake(ALICE_UID),
      severity: "GREEN_FAKE",
    })
  );
});

test("Leaderboard allows signed-in list but blocks cross-user writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      cefrLevel: "B1+",
      xp: 350,
      weeklyPoints: 120,
      streakDays: 4,
      badgeTitle: "Rising Scholar",
      updatedAt: pastDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("leaderboard").get());
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({ xp: 0, updatedAt: pastDate() })
  );
});

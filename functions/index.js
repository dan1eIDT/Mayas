const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendMessageNotification =
  functions.firestore
      .document("chats/{chatId}/messages/{messageId}")
      .onCreate(async (snap, context) => {
        const message = snap.data();

        const receiverUid = message.receiverUid;

        const userDoc = await admin.firestore()
            .collection("users")
            .doc(receiverUid)
            .get();

        const userData = userDoc.data();

        if (!userData) return null;

        const token = userData.fcmToken;

        const payload = {
          notification: {
            title: message.senderName || "MAYAS",
            body: message.text || "Новое сообщение",
          },
          data: {
            senderName: message.senderName || "MAYAS",
            text: message.text || "",
          },
        };

        return admin.messaging().send({
          token: token,
          notification: payload.notification,
          data: payload.data,
        });
      });
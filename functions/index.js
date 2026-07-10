const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({region: "southamerica-east1"});

exports.notifyNewAppointment = onDocumentCreated("appointments/{id}", async (event) => {
  const appt = event.data.data();
  try {
    // Notifica o Salão
    const profile = await admin.firestore().collection("profiles").doc(appt.salonId).get();
    if (profile.exists && profile.data().fcmToken) {
      const isConfirmed = appt.status === "CONFIRMED";
      const title = isConfirmed ? "Agendamento Confirmado! ✅" : "Novo Agendamento! 📅";
      const body = isConfirmed 
        ? `Novo agendamento confirmado automaticamente: ${appt.clientName} agendou ${appt.serviceName}.`
        : `${appt.clientName} agendou ${appt.serviceName}.`;

      const message = {
        notification: { title, body },
        token: profile.data().fcmToken,
        android: {
          priority: "high",
          notification: {channelId: "agendamentos_channel"},
        },
      };
      await admin.messaging().send(message);
    }

    // Notifica o Cliente caso tenha sido aceito automaticamente
    if (appt.status === "CONFIRMED") {
      const clientProfile = await admin.firestore().collection("profiles").doc(appt.clientUid).get();
      if (clientProfile.exists && clientProfile.data().fcmToken) {
        const salon = await admin.firestore().collection("salons").doc(appt.salonId).get();
        const salonName = salon.exists ? salon.data().name : "O Salão";

        const clientMessage = {
          notification: {
            title: "Agendamento Confirmado! ✅",
            body: `Seu horário de ${appt.serviceName} no ${salonName} foi confirmado automaticamente.`
          },
          token: clientProfile.data().fcmToken,
          android: {
            priority: "high",
            notification: {channelId: "agendamentos_channel"},
          },
        };
        await admin.messaging().send(clientMessage);
      }
    }
  } catch (e) {
    console.error(e);
  }
  return null;
});

exports.notifyStatusChanged = onDocumentUpdated("appointments/{id}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();

  if (before.status !== "CONFIRMED" && after.status === "CONFIRMED") {
    try {
      const profile = await admin.firestore().collection("profiles").doc(after.clientUid).get();
      const salon = await admin.firestore().collection("salons").doc(after.salonId).get();
      const salonName = salon.exists ? salon.data().name : "O Salão";

      if (!profile.exists || !profile.data().fcmToken) return null;

      const message = {
        notification: {
          title: "Agendamento Confirmado! ✅",
          body: `${salonName} aceitou seu horário de ${after.serviceName}.`,
        },
        token: profile.data().fcmToken,
        android: {
          priority: "high",
          notification: {channelId: "agendamentos_channel"},
        },
      };

      await admin.messaging().send(message);
    } catch (e) {
      console.error(e);
    }
  }
  return null;
});

exports.notifyBroadcast = onDocumentCreated("broadcasts/{id}", async (event) => {
  const broadcast = event.data.data();
  try {
    const message = {
      notification: {
        title: broadcast.title,
        body: broadcast.message,
      },
      topic: broadcast.target,
      android: {
        priority: "high",
        notification: {channelId: "agendamentos_channel"},
      },
    };

    await admin.messaging().send(message);
    console.log(`Broadcast successfully sent to topic: ${broadcast.target}`);
  } catch (e) {
    console.error(e);
  }
  return null;
});

exports.notifySalonPromotion = onDocumentCreated("salons/{salonId}/promotions/{promoId}", async (event) => {
  const promo = event.data.data();
  const salonId = event.params.salonId;
  try {
    const salon = await admin.firestore().collection("salons").doc(salonId).get();
    const salonName = salon.exists ? salon.data().name : "Salão";

    const message = {
      notification: {
        title: `${salonName}: ${promo.title || "Novidade!"}`,
        body: promo.message,
      },
      topic: `salon_${salonId}`,
      android: {
        priority: "high",
        notification: {channelId: "agendamentos_channel"},
      },
    };

    await admin.messaging().send(message);
    console.log(`Promotion push sent to topic salon_${salonId}`);
  } catch (e) {
    console.error(e);
  }
  return null;
});

exports.notifySupportMessage = onDocumentCreated("support_chats/{chatId}/messages/{messageId}", async (event) => {
  const msg = event.data.data();
  const chatId = event.params.chatId;
  try {
    if (msg.sender === "ADMIN") {
      // Notifica o usuário de que o suporte respondeu
      const profile = await admin.firestore().collection("profiles").doc(chatId).get();
      if (profile.exists && profile.data().fcmToken) {
        const message = {
          notification: {
            title: "Suporte Agendei PRO 👑",
            body: msg.text,
          },
          token: profile.data().fcmToken,
          android: {
            priority: "high",
            notification: {channelId: "agendamentos_channel"},
          },
        };
        await admin.messaging().send(message);
        console.log(`Support response push sent to user ${chatId}`);
      }
    } else if (msg.sender === "USER") {
      // Notifica o admin imperial sobre nova mensagem se o chamado estiver na fila humana
      const chatDoc = await admin.firestore().collection("support_chats").doc(chatId).get();
      const chatData = chatDoc.exists ? chatDoc.data() : null;
      if (chatData && chatData.status === "PENDING_HUMAN") {
        const message = {
          notification: {
            title: "Novo chamado de suporte! 🙋‍♂️",
            body: `${chatData.userName} (${chatData.userType === "SALON" ? "Salão" : "Cliente"}): ${msg.text}`,
          },
          topic: "admin_support",
          android: {
            priority: "high",
            notification: {channelId: "agendamentos_channel"},
          },
        };
        await admin.messaging().send(message);
        console.log(`Support request push sent to admins for chat ${chatId}`);
      }
    }
  } catch (e) {
    console.error(e);
  }
  return null;
});

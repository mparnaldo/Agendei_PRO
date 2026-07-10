const admin = require('firebase-admin');
const serviceAccount = require('./firebase-admin-key.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function deleteCollection(collectionPath, batchSize = 100) {
  const collectionRef = db.collection(collectionPath);
  const query = collectionRef.orderBy('__name__').limit(batchSize);

  return new Promise((resolve, reject) => {
    deleteQueryBatch(db, query, resolve, reject);
  });
}

async function deleteQueryBatch(db, query, resolve, reject) {
  try {
    const snapshot = await query.get();

    const batchSize = snapshot.size;
    if (batchSize === 0) {
      resolve();
      return;
    }

    const batch = db.batch();
    snapshot.docs.forEach((doc) => {
      batch.delete(doc.ref);
    });

    await batch.commit();

    // Recurse on next tick
    process.nextTick(() => {
      deleteQueryBatch(db, query, resolve, reject);
    });
  } catch (error) {
    reject(error);
  }
}

async function clearAll() {
  const collections = ['appointments', 'waiting_list', 'user_bindings', 'profiles', 'notifications'];
  
  console.log('Buscando salões para limpar subcoleções de serviços...');
  const salonsSnapshot = await db.collection('salons').get();
  for (const salonDoc of salonsSnapshot.docs) {
    console.log(`Limpando subcoleção 'services' para o salão ${salonDoc.id}...`);
    const servicesSnapshot = await salonDoc.ref.collection('services').get();
    if (servicesSnapshot.size > 0) {
      const batch = db.batch();
      servicesSnapshot.docs.forEach(doc => batch.delete(doc.ref));
      await batch.commit();
    }
    
    // Deleta o próprio salão
    console.log(`Limpando salão ${salonDoc.id}...`);
    await salonDoc.ref.delete();
  }
  console.log('Salões e subcoleções de serviços limpos!');

  for (const collection of collections) {
    console.log(`Limpando coleção: ${collection}...`);
    await deleteCollection(collection);
  }
  
  console.log('Banco de dados Firestore totalmente limpo e zerado!');
}

clearAll().catch(err => {
  console.error('Erro ao limpar o Firestore:', err);
  process.exit(1);
});

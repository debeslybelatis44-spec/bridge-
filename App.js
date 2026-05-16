import React, { useRef } from 'react';
import { WebView } from 'react-native-webview';
import RNPrint from 'react-native-print';
import { Alert } from 'react-native';

export default function App() {
  const webviewRef = useRef(null);

  const handleMessage = async (event) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      if (data.type === 'PRINT_HTML') {
        await RNPrint.print({ html: data.html });
        webviewRef.current?.postMessage(JSON.stringify({ type: 'PRINT_SUCCESS' }));
      }
    } catch (err) {
      console.error(err);
      Alert.alert('Erreur', "Échec de l'impression. Vérifiez l'imprimante.");
    }
  };

  const injectedJS = `
    (function() {
      if (window.ReactNativeWebView) {
        window.printHTMLContent = function(html, title) {
          window.ReactNativeWebView.postMessage(JSON.stringify({
            type: 'PRINT_HTML',
            html: html,
            title: title || 'Ticket'
          }));
        };
        console.log('✅ Pont d\\'impression actif');
      }
    })();
  `;

  return (
    <WebView
      ref={webviewRef}
      source={{ uri: 'https://lotato1.onrender.com' }}
      onMessage={handleMessage}
      injectedJavaScriptBeforeContentLoaded={injectedJS}
      javaScriptEnabled
      domStorageEnabled
      startInLoadingState
      mixedContentMode="always"
    />
  );
}

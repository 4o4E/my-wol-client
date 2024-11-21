package top.e404.mywol.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.e404.mywol.model.WolClient

@Composable
fun ClientItem(client: WolClient) {
    Text(client.name)
    Column(Modifier.fillMaxWidth()) {
        for (machine in client.machines) {
            RemoteMachineItem(client, machine)
        }
    }
}
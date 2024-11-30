package top.e404.mywol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.e404.mywol.model.WolClient
import top.e404.mywol.vm.UiVm

@Composable
fun ClientItem(client: WolClient) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(client.name, fontSize = 30.sp)
            if (UiVm.debug) Text(client.id)
        }
    }
    Column(Modifier.fillMaxWidth()) {
        for (machine in client.machines) {
            MachineItem(RemoteMachineWrapper(client, machine))
        }
    }
}
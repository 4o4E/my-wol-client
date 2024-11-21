package top.e404.mywol.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.e404.mywol.ui.components.LocalMachineItem
import top.e404.mywol.vm.LocalVm

@Composable
fun Local() {
    val vm = LocalVm

    val list by vm.itemList.collectAsState()

    Column {
        LazyColumn(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(10.dp)) {
            items(list.size) { LocalMachineItem(list[it]) }
        }
    }
}
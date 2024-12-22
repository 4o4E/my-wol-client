package top.e404.mywol.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.e404.mywol.ui.components.LocalMachineWrapper
import top.e404.mywol.ui.components.MachineItem
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.UiVm

@Composable
fun Local() {
    val list by LocalVm.itemList.collectAsState()
    var showAdd by remember { UiVm.showAddMachine }
    var editMachine by remember { UiVm.editMachine }
    val modalBottomSheetState = rememberModalBottomSheetState(true) { true }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { showAdd = true }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null
                )
            }
            if (showAdd) {
                ModalBottomSheet(
                    sheetState = modalBottomSheetState,
                    modifier = Modifier.fillMaxWidth(),
                    onDismissRequest = { showAdd = false }
                ) {
                    EditMachine()
                }
            }
            if (editMachine != null) {
                ModalBottomSheet(
                    sheetState = modalBottomSheetState,
                    modifier = Modifier.fillMaxWidth(),
                    onDismissRequest = { editMachine = null }
                ) {
                    EditMachine()
                }
            }
        }
    ) {
        Column {
            LazyColumn(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(10.dp)
            ) {
                items(list.size) { MachineItem(LocalMachineWrapper(list[it])) }
            }
        }
    }
}
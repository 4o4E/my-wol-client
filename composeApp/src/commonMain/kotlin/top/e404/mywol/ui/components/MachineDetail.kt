package top.e404.mywol.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.e404.mywol.dao.Machine
import top.e404.mywol.vm.LocalVm

/**
 * 设备详情
 */
@Composable
fun MachineDetail(machine: Machine) {
    Column(Modifier.fillMaxSize().clickable { LocalVm.detailMachineAnimation.value = false }) {
        // 设备信息
        Text(text = machine.name)
        // 操作按钮

        // ssh记录
    }
}
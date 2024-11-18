package top.e404.mywol.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import top.e404.mywol.dao.Machine
import top.e404.mywol.ui.components.MachineItem
import java.util.UUID

@Preview
@Composable
fun MachineItemPreview() = MachineItem(
    Machine(
        UUID.randomUUID().toString(),
        "name",
        "1.1.1.1",
        "1.1.1.1",
        "aa:aa:aa:aa:aa:aa",
        System.currentTimeMillis()
    )
)